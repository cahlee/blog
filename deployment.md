# Deployment Guide

## 아키텍처

```
사용자 브라우저
    ↓ HTTPS
Cloudflare (DNS Only - 프록시 OFF)
    ↓
GCP HTTPS Load Balancer (Google 관리 SSL 인증서)
    ↓ HTTP:8080
Compute Engine VM (e2-small, 서울)
  └─ Spring Boot JAR
       └─ Cloud SQL MySQL 8.0 (Private IP)
       └─ Cloud Storage (이미지 업로드)
```

## 환경 정보

| 항목 | 값 |
|------|----|
| GCP 리전 | asia-northeast3 (서울) |
| Zone | asia-northeast3-a |
| 도메인 | blog.cahlee.io |
| VM | e2-small, Ubuntu 22.04 LTS |
| DB | Cloud SQL MySQL 8.0 (db-f1-micro) |
| Java | OpenJDK 21 |

---

## 1단계. GCP 기본 설정

```bash
gcloud auth login
gcloud config set project [PROJECT_ID]
gcloud config set compute/region asia-northeast3
gcloud config set compute/zone asia-northeast3-a

# 필요한 API 활성화
gcloud services enable compute.googleapis.com \
  sqladmin.googleapis.com \
  storage.googleapis.com \
  servicenetworking.googleapis.com
```

---

## 2단계. Global 고정 IP 예약

Load Balancer는 Regional IP가 아닌 Global IP가 필요합니다.

```bash
gcloud compute addresses create blog-global-ip --global

# Cloudflare DNS에 등록할 IP 확인
gcloud compute addresses describe blog-global-ip \
  --global --format="get(address)"
```

---

## 3단계. Cloud Storage 버킷 생성

```bash
gcloud storage buckets create gs://blog-cahlee-images \
  --location=asia-northeast3 \
  --uniform-bucket-level-access
```

---

## 4단계. Cloud SQL MySQL 생성 (Private IP 전용)

외부에서 직접 접근 불가, VM과 같은 VPC 내에서만 접근 가능합니다.

```bash
# VPC 피어링 설정 (Private IP 사용을 위해 필요)
gcloud compute addresses create google-managed-services-default \
  --global \
  --purpose=VPC_PEERING \
  --prefix-length=16 \
  --network=default

gcloud services vpc-peerings connect \
  --service=servicenetworking.googleapis.com \
  --ranges=google-managed-services-default \
  --network=default

# Cloud SQL 인스턴스 생성
gcloud sql instances create blog-db \
  --database-version=MYSQL_8_0 \
  --tier=db-f1-micro \
  --region=asia-northeast3 \
  --network=default \
  --no-assign-ip \
  --availability-type=zonal

# DB 및 유저 생성
gcloud sql databases create blog --instance=blog-db

gcloud sql users create bloguser \
  --instance=blog-db \
  --password=[DB_PASSWORD]

# Private IP 확인 (환경변수에 입력할 값)
gcloud sql instances describe blog-db \
  --format="get(ipAddresses[0].ipAddress)"
```

---

## 5단계. Compute Engine VM 생성

```bash
gcloud compute instances create blog-server \
  --zone=asia-northeast3-a \
  --machine-type=e2-small \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --tags=http-server,https-server \
  --scopes=cloud-platform

# 방화벽 - 인터넷에서 80, 443 허용
gcloud compute firewall-rules create allow-web \
  --allow=tcp:80,tcp:443 \
  --target-tags=http-server,https-server

# 방화벽 - Load Balancer 헬스체크 허용 (필수)
gcloud compute firewall-rules create allow-lb-health-check \
  --allow=tcp:8080 \
  --source-ranges=130.211.0.0/22,35.191.0.0/16 \
  --target-tags=http-server
```

---

## 6단계. VM 초기 설정

```bash
# VM에 SSH 접속
gcloud compute ssh blog-server --zone=asia-northeast3-a

# Java 21 설치
sudo apt update && sudo apt install -y openjdk-21-jre-headless

# 앱 디렉토리 생성
sudo mkdir -p /opt/blog
sudo chown $USER:$USER /opt/blog
```

---

## 7단계. 서비스 계정 IAM 권한 설정

VM에서 Cloud Storage에 파일을 업로드하려면 권한이 필요합니다.

```bash
# VM 외부에서 실행
PROJECT_ID=$(gcloud config get-value project)
SERVICE_ACCOUNT=$(gcloud compute instances describe blog-server \
  --zone=asia-northeast3-a \
  --format="get(serviceAccounts[0].email)")

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/storage.objectAdmin"
```

---

## 8단계. JAR 빌드 및 업로드

로컬에서 실행합니다.

```bash
./mvnw package -DskipTests

gcloud compute scp target/blog-0.0.1-SNAPSHOT.jar \
  blog-server:/opt/blog/blog.jar \
  --zone=asia-northeast3-a
```

---

## 9단계. 환경변수 및 systemd 서비스 등록

VM에서 실행합니다.

```bash
sudo nano /opt/blog/.env
```

```ini
SPRING_PROFILES_ACTIVE=prod
DB_USERNAME=bloguser
DB_PASSWORD=[설정한 비밀번호]
JWT_SECRET=[32자 이상 랜덤 문자열]
GOOGLE_CLIENT_ID=[Google OAuth2 Client ID]
GOOGLE_CLIENT_SECRET=[Google OAuth2 Client Secret]
GITHUB_CLIENT_ID=[GitHub OAuth App Client ID]
GITHUB_CLIENT_SECRET=[GitHub OAuth App Client Secret]
GCP_BUCKET_NAME=blog-cahlee-images
GCP_CREDENTIALS_PATH=
```

```bash
# .env 파일 권한 제한 (소유자만 읽기)
chmod 600 /opt/blog/.env
```

```bash
sudo nano /etc/systemd/system/blog.service
```

```ini
[Unit]
Description=Blog Spring Boot App
After=network.target

[Service]
User=ubuntu
EnvironmentFile=/opt/blog/.env
ExecStart=/usr/bin/java -jar /opt/blog/blog.jar \
  --spring.datasource.url=jdbc:mysql://[CLOUD_SQL_PRIVATE_IP]:3306/blog?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog

# 정상 기동 확인
sudo systemctl status blog
sudo journalctl -u blog -f
```

---

## 10단계. Load Balancer 구성

### VM을 Instance Group으로 등록

```bash
gcloud compute instance-groups unmanaged create blog-instance-group \
  --zone=asia-northeast3-a

gcloud compute instance-groups unmanaged add-instances blog-instance-group \
  --instances=blog-server \
  --zone=asia-northeast3-a

gcloud compute instance-groups set-named-ports blog-instance-group \
  --named-ports=http:8080 \
  --zone=asia-northeast3-a
```

### 헬스체크 생성

```bash
gcloud compute health-checks create http blog-health-check \
  --port=8080 \
  --request-path=/
```

### 백엔드 서비스 생성

```bash
gcloud compute backend-services create blog-backend \
  --protocol=HTTP \
  --port-name=http \
  --health-checks=blog-health-check \
  --global

gcloud compute backend-services add-backend blog-backend \
  --instance-group=blog-instance-group \
  --instance-group-zone=asia-northeast3-a \
  --global
```

### URL Map 생성

```bash
gcloud compute url-maps create blog-url-map \
  --default-service=blog-backend
```

### Google 관리 SSL 인증서 생성

```bash
gcloud compute ssl-certificates create blog-ssl-cert \
  --domains=blog.cahlee.io \
  --global
```

> Cloudflare DNS Only 설정 후 트래픽이 LB로 들어오기 시작하면 자동 발급됩니다 (최대 수십 분 소요).

### HTTPS 프록시 및 포워딩 룰

```bash
gcloud compute target-https-proxies create blog-https-proxy \
  --url-map=blog-url-map \
  --ssl-certificates=blog-ssl-cert \
  --global

gcloud compute forwarding-rules create blog-https-rule \
  --address=blog-global-ip \
  --target-https-proxy=blog-https-proxy \
  --ports=443 \
  --global
```

### HTTP → HTTPS 리다이렉트

```bash
gcloud compute url-maps import blog-http-redirect --global << 'EOF'
kind: compute#urlMap
name: blog-http-redirect
defaultUrlRedirect:
  redirectResponseCode: MOVED_PERMANENTLY_DEFAULT
  httpsRedirect: true
EOF

gcloud compute target-http-proxies create blog-http-proxy \
  --url-map=blog-http-redirect \
  --global

gcloud compute forwarding-rules create blog-http-rule \
  --address=blog-global-ip \
  --target-http-proxy=blog-http-proxy \
  --ports=80 \
  --global
```

---

## 11단계. Cloudflare DNS 설정

1. Cloudflare 대시보드 → cahlee.io → DNS
2. A 레코드 추가

| Type | Name | Content | Proxy status |
|------|------|---------|--------------|
| A | blog | [Global IP] | **DNS only (회색 구름)** |

> **주의:** Proxy(주황 구름)가 켜져 있으면 Google이 도메인 소유권을 검증하지 못해 인증서 발급이 실패합니다. 반드시 DNS Only로 설정하세요.

---

## 12단계. OAuth2 Redirect URI 등록

### Google Cloud Console

1. APIs & Services → Credentials → OAuth 2.0 Client IDs
2. Authorized redirect URIs 추가:
   - `https://blog.cahlee.io/login/oauth2/code/google`

### GitHub

1. Settings → Developer settings → OAuth Apps
2. Authorization callback URL 추가:
   - `https://blog.cahlee.io/login/oauth2/code/github`

---

## 13단계. application-prod.yml 추가 설정

Load Balancer가 HTTPS를 처리하고 VM에는 HTTP로 전달하므로, 리다이렉트 URL이 올바르게 생성되도록 설정합니다.

```yaml
server:
  forward-headers-strategy: framework
```

---

## 인증서 발급 확인

```bash
gcloud compute ssl-certificates describe blog-ssl-cert \
  --global \
  --format="get(managed.status, managed.domainStatus)"
```

`ACTIVE` 상태가 되면 `https://blog.cahlee.io` 접속이 가능합니다.

---

## 업데이트 배포 방법

새 버전 배포 시 아래 순서로 진행합니다.

```bash
# 1. 로컬에서 빌드
./mvnw package -DskipTests

# 2. VM에 JAR 업로드
gcloud compute scp target/blog-0.0.1-SNAPSHOT.jar \
  blog-server:/opt/blog/blog.jar \
  --zone=asia-northeast3-a

# 3. 서비스 재시작
gcloud compute ssh blog-server --zone=asia-northeast3-a \
  --command="sudo systemctl restart blog"

# 4. 로그 확인
gcloud compute ssh blog-server --zone=asia-northeast3-a \
  --command="sudo journalctl -u blog -n 50"
```

---

## 트러블슈팅

### 앱 로그 확인
```bash
gcloud compute ssh blog-server --zone=asia-northeast3-a
sudo journalctl -u blog -f
```

### 헬스체크 상태 확인
```bash
gcloud compute backend-services get-health blog-backend --global
```

### 인증서 상태 확인
```bash
gcloud compute ssl-certificates describe blog-ssl-cert \
  --global \
  --format="get(managed.status, managed.domainStatus)"
```

### Cloud SQL 연결 확인 (VM 내부에서)
```bash
mysql -h [CLOUD_SQL_PRIVATE_IP] -u bloguser -p blog
```

---

## 홈서버 배포 (OpenStack, homeserver 프로필)

> Ubuntu 단일 인스턴스 + 블록스토리지 마운트 환경. GCP 없이 MySQL + 로컬 파일 저장소 사용.
> OAuth2(Google/GitHub) 로그인은 도메인 연결 후 추가 예정. 현재는 폼 로그인만 사용.

---

### 1단계. MySQL 초기 설정

```bash
# MySQL 접속 (root)
sudo mysql
```

```sql
-- DB 생성
CREATE DATABASE blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 전용 계정 생성 및 권한 부여
CREATE USER 'bloguser'@'localhost' IDENTIFIED BY '[비밀번호]';
GRANT ALL PRIVILEGES ON blog.* TO 'bloguser'@'localhost';
FLUSH PRIVILEGES;

EXIT;
```

```bash
# 연결 확인
mysql -u bloguser -p blog
```

> MySQL이 동일 인스턴스에 있는 경우 `DB_HOST=127.0.0.1`을 사용합니다.
> `localhost`는 소켓 연결을 시도하므로 JDBC에서는 `127.0.0.1` 권장.

---

### 2단계. 디렉토리 및 블록스토리지 준비

```bash
# 블록스토리지 마운트 확인 (예: /mnt/data)
df -h

# 업로드 디렉토리 생성 (블록스토리지 경로에)
mkdir -p /opt/blog/uploads

# 앱 디렉토리 생성
sudo mkdir -p /opt/blog
sudo chown $USER:$USER /opt/blog
```

---

### 3단계. JAR 빌드 및 업로드

```bash
# 로컬에서 빌드
./mvnw package -DskipTests

# 서버로 전송
scp target/blog-0.0.1-SNAPSHOT.jar ubuntu@192.168.0.229:/opt/blog/blog.jar
```

---

### 4단계. 환경변수 파일 생성

```bash
sudo nano /opt/blog/.env
```

```ini
SPRING_PROFILES_ACTIVE=homeserver

# DB 연결 정보
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=blog
DB_USERNAME=bloguser
DB_PASSWORD=[설정한 비밀번호]

# 파일 저장소
UPLOAD_DIR=/opt/blog/uploads
UPLOAD_BASE_URL=http://ctrl0703.iptime.org:8081/uploads

# 보안
JWT_SECRET=[32자 이상 랜덤 문자열]
```

```bash
chmod 600 /opt/blog/.env
```

> JWT_SECRET 생성 예시: `openssl rand -base64 32`

---

### 5단계. systemd 서비스 등록

```bash
sudo nano /etc/systemd/system/blog.service
```

```ini
[Unit]
Description=Blog Spring Boot App
After=network.target

[Service]
User=ubuntu
EnvironmentFile=/opt/blog/.env
ExecStart=/usr/bin/java -jar /opt/blog/blog.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable blog
sudo systemctl start blog

# 기동 확인
sudo systemctl status blog
sudo journalctl -u blog -f
```

---

### 6단계. nginx 리버스 프록시 설정

```bash
sudo apt update && sudo apt install -y nginx
```

```bash
sudo nano /etc/nginx/sites-available/blog
```

```nginx
server {
    listen 80;
    server_name 192.168.0.229;  # 도메인 연결 시 도메인으로 교체

    # 업로드 파일 직접 서빙 (Spring Boot를 거치지 않아 효율적)
    location /uploads/ {
        alias /opt/blog/uploads/;
    }

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# 설정 활성화
sudo ln -s /etc/nginx/sites-available/blog /etc/nginx/sites-enabled/
sudo nginx -t          # 설정 문법 확인
sudo systemctl enable nginx
sudo systemctl restart nginx
```

```bash
# 방화벽 설정
sudo ufw allow 80/tcp
sudo ufw allow 22/tcp  # SSH 차단 방지
sudo ufw enable
sudo ufw status
```

> nginx가 80포트를 받아 Spring Boot(8080)로 전달합니다.
> Spring Boot 8080은 외부에 직접 노출하지 않아도 됩니다.

---

### 업데이트 배포

```bash
# 1. 로컬에서 빌드
./mvnw package -DskipTests

# 2. 서버로 전송
scp target/blog-0.0.1-SNAPSHOT.jar ubuntu@192.168.0.229:/opt/blog/blog.jar

# 3. 서비스 재시작
ssh ubuntu@192.168.0.229 "sudo systemctl restart blog"

# 4. 로그 확인
ssh ubuntu@192.168.0.229 "sudo journalctl -u blog -n 50"
```

---

### 트러블슈팅

```bash
# 앱 로그
sudo journalctl -u blog -f

# nginx 로그
sudo tail -f /var/log/nginx/error.log

# DB 연결 확인
mysql -u bloguser -p blog -h 127.0.0.1
```
