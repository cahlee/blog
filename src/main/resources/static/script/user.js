const submit = document.querySelector(".user__submit");
submit.addEventListener("click", () => {
  // validation

  // submit
  const url = "http://localhost:8080/api/users";
  const params = {
    id: document.getElementsByName("id")[0].value,
    name: document.getElementsByName("name")[0].value,
    password: document.getElementsByName("password")[0].value
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

const cancel = document.querySelector(".user__cancel");
cancel.addEventListener("click", () => {
	window.location.href = "/";
});