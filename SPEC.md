# Blog 프로젝트 요구사항 및 시스템 명세

## 1. 프로젝트 개요

개인 블로그 플랫폼. 사용자별 고유 URL 공간을 가지며, Markdown 기반 포스팅을 지원한다.
Thymeleaf 기반 서버 사이드 렌더링으로 구현하되, 추후 SPA 전환을 위한 REST API를 병행 제공한다.

---

## 2. 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 |
| Build | Maven (mvnw) |
| ORM | Spring Data JPA + Hibernate |
| Frontend | Thymeleaf + Bootstrap 5.3 |
| Markdown Editor | Toast UI Editor (CDN) |
| Markdown Renderer | Flexmark (서버 사이드) |
| Auth | Spring Security (세션) + OAuth2 (Google, GitHub) |
| DB (local) | H2 in-memory (MODE=MySQL) |
| DB (prod) | MySQL |
| Storage (local) | 로컬 파일시스템 |
| Storage (prod) | GCP Cloud Storage |

---

## 3. URL 구조

### 페이지 (Thymeleaf)

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/` | 로그인 → `/{username}` 리다이렉트, 비로그인 → `/auth/login` 리다이렉트 | - |
| GET | `/auth/login` | 로그인 페이지 | 불필요 |
| GET | `/auth/register` | 회원가입 페이지 | 불필요 |
| GET | `/{username}` | 사용자 블로그 홈 (포스트 목록) | 불필요 |
| GET | `/{username}/{id}` | 포스트 상세 | 불필요 (비공개 글은 본인만) |
| GET | `/posts/new` | 포스트 작성 폼 | 필요 |
| POST | `/posts/new` | 포스트 저장 | 필요 |
| GET | `/{username}/{id}/edit` | 포스트 수정 폼 | 필요 (본인만) |
| POST | `/{username}/{id}/edit` | 포스트 수정 저장 | 필요 (본인만) |
| POST | `/{username}/{id}/delete` | 포스트 삭제 | 필요 (본인만) |
| GET | `/posts/search` | 전체 포스트 검색 결과 | 불필요 |
| POST | `/{username}/{id}/comments` | 댓글 작성 | 불필요 (비회원 가능) |
| POST | `/{username}/{id}/comments/{commentId}/delete` | 댓글 삭제 | 불필요 (본인/비회원 비밀번호) |
| GET | `/categories` | 카테고리 관리 | 필요 |
| POST | `/categories` | 카테고리 생성 | 필요 |
| POST | `/categories/{id}/edit` | 카테고리 수정 | 필요 |
| POST | `/categories/{id}/delete` | 카테고리 삭제 | 필요 |
| GET | `/profile` | 프로필 페이지 | 필요 |
| POST | `/profile/update` | 프로필 수정 | 필요 |
| POST | `/profile/change-password` | 비밀번호 변경 | 필요 |

### REST API

| Method | URL | 설명 | 인증 |
|--------|-----|------|------|
| GET | `/api/posts` | 공개 포스트 목록 (페이징) | 불필요 |
| GET | `/api/posts?username={username}` | 특정 유저의 공개 포스트 목록 | 불필요 |
| GET | `/api/posts?q={keyword}` | 포스트 검색 | 불필요 |
| GET | `/api/posts/{id}` | 포스트 상세 | 불필요 (비공개는 본인만) |
| POST | `/api/posts` | 포스트 생성 | 필요 |
| PUT | `/api/posts/{id}` | 포스트 수정 | 필요 (본인만) |
| DELETE | `/api/posts/{id}` | 포스트 삭제 (204) | 필요 (본인만) |
| POST | `/api/images/upload` | 이미지 업로드 | 필요 |

> 미인증 상태로 `/api/**` 접근 시 HTTP 401 반환 (로그인 페이지 리다이렉트 없음)

---

## 4. 데이터 모델

### User
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK, Auto Increment |
| email | String | Unique, Not Null |
| username | String | Unique, Not Null (URL에 사용) |
| password | String | Nullable (OAuth2 유저는 없음) |
| role | Enum(USER, ADMIN) | Not Null |
| provider | Enum(LOCAL, GOOGLE, GITHUB) | Not Null |
| providerId | String | Nullable |
| createdAt | LocalDateTime | Auto |

### Post
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK, Auto Increment |
| title | String | Not Null, Max 200자 |
| content | TEXT | Not Null (Markdown 원문) |
| isPublic | boolean | Not Null, 기본값 true |
| viewCount | int | Not Null, 기본값 0 |
| createdAt | LocalDateTime | Auto |
| updatedAt | LocalDateTime | Auto |
| user | User | FK, Not Null |
| category | Category | FK, Nullable |
| tags | Set\<Tag\> | ManyToMany |

### Comment
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK, Auto Increment |
| content | String | Not Null, Max 1000자 |
| isDeleted | boolean | Not Null, 기본값 false |
| createdAt | LocalDateTime | Auto |
| post | Post | FK, Not Null |
| user | User | FK, Nullable (비회원이면 null) |
| guestName | String | Nullable (비회원용) |
| guestPassword | String | Nullable (비회원용, BCrypt) |

### Category
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| name | String | Not Null |
| user | User | FK, Not Null |

### Tag
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| name | String | Unique, Not Null |

---

## 5. 기능 요구사항

### 5.1 인증

- **로컬 로그인**: 이메일 + 비밀번호 (BCrypt)
- **OAuth2 로그인**: Google, GitHub
- 동일 이메일로 provider 관계없이 중복 가입 불가
- 세션 기반 인증 (Spring Security 기본)

### 5.2 포스트

- 제목(최대 200자), 내용(Markdown), 카테고리, 태그, 공개 여부 설정
- Toast UI Editor로 Markdown 작성, 서버에서 Flexmark로 HTML 렌더링
- 비공개 포스트는 작성자 본인만 열람
- 조회수 자동 증가 (상세 조회 시)
- 태그는 존재하지 않으면 자동 생성 (전체 유저 공유)
- 카테고리는 유저별 독립 관리

### 5.3 댓글

- 1depth (대댓글 없음)
- 로그인 유저: 계정으로 작성, 본인 댓글만 삭제 가능
- 비회원: guestName + guestPassword로 작성, 비밀번호 입력으로 삭제
- 삭제된 댓글은 내용을 숨기고 "삭제된 댓글" 표시

### 5.4 검색

- 전체 공개 포스트 대상 (제목, 내용, 태그)
- 프로덕션: MySQL FULLTEXT 검색
- 로컬/H2: LIKE 검색으로 자동 폴백

### 5.5 이미지 업로드

- 포스트 작성 중 에디터에서 이미지 삽입 시 업로드
- 로컬: 파일시스템 저장 (`~/blog-uploads/`)
- 프로덕션: GCP Cloud Storage

### 5.6 유저 블로그 홈 (`/{username}`)

- 해당 유저의 포스트 목록 표시
- 본인이 접근하면 비공개 포스트 포함 표시
- 타인이 접근하면 공개 포스트만 표시

---

## 6. 보안 정책

- GET 요청은 전체 공개 (인증 필요 페이지 제외)
- 상태 변경(POST/PUT/DELETE)은 인증 필요
- `/api/**` 미인증 시 401 반환 (리다이렉트 없음)
- CSRF: Thymeleaf 폼에 토큰 포함, `/api/**`는 CSRF 비활성화
- 포스트/댓글 수정·삭제는 본인 소유 확인

---

## 7. 환경별 설정

| 구분 | local | prod |
|------|-------|------|
| DB | H2 in-memory | MySQL |
| DDL | create-drop | (별도 설정) |
| Storage | 로컬 파일시스템 | GCP Cloud Storage |
| 테스트 계정 | DataInitializer로 자동 생성 | 없음 |

---

## 8. 미구현 / 추후 검토

- 임시저장(draft) 기능
- 댓글 REST API
- SPA 전환 시 JWT 기반 인증으로 교체
- 클라우드 배포 설정 (GCP 등)
- 유저 프로필 이미지
- 팔로우 / 구독 기능
