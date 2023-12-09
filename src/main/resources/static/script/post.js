const Editor = toastui.Editor;

const editor = new Editor({
  el: document.querySelector("#editor"),
  height: "500px",
  initialEditType: "markdown",
  previewStyle: "vertical",
});

const submit = document.querySelector(".editor__submit");
submit.addEventListener("click", () => {
  // validation

  // submit
  const url = "http://localhost:8080/api/posts";
  const params = {
    title: document.getElementsByName("title")[0].value,
    author: document.getElementsByName("author")[0].value,
    contents: editor.getHTML(),
  };

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(params),
  }).then((response) => {
    if (response.ok) {
      alert("등록이 완료되었습니다");
      window.location.href = "/";
    }
  });
});
