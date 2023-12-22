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