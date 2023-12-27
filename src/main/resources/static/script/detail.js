// 목록
const listButton = document.querySelector(".viewer__list");
listButton.addEventListener("click", () => {
	window.location.href = "/post/list";
});

// 수정
const updateButton = document.querySelector(".viewer__update");
updateButton.addEventListener("click", () => {
	const postId = document.querySelector("#postId");
	window.location.href = "/post/update?postId=" + postId.value;
});

// 삭제
const deleteButton = document.querySelector(".viewer__delete");
deleteButton.addEventListener("click", () => {
  // validation

  // submit
  const url = "http://localhost:8080/api/posts" + "/" + document.querySelector("#postId").value;

  fetch(url, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
  }).then((response) => {
    if (response.ok) {
      alert("삭제가 완료되었습니다");
      window.location.href = "/";
    }
  });
});