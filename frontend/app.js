const API_BASE = window.EXAM_API_BASE.replace(/\/$/, "");
const app = document.getElementById("app");
const TAB_AUTH_KEY = "exam.tabAuthenticated";

let currentUser = null;
let meta = null;
let timerId = null;

const roleHome = {
    STUDENT: "#/student",
    TEACHER: "#/teacher",
    ADMIN: "#/admin"
};

const roleBrand = {
    STUDENT: "学生考试中心",
    TEACHER: "教师工作台",
    ADMIN: "系统管理中心"
};

const roleInitial = {
    STUDENT: "S",
    TEACHER: "T",
    ADMIN: "A"
};

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function formatTime(value) {
    if (!value) {
        return "-";
    }
    return new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

async function api(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data?.message || "请求失败");
    }
    return data;
}

function setHash(hash) {
    if (location.hash === hash) {
        renderRoute();
        return;
    }
    location.hash = hash;
}

function optionList(options, selected) {
    return options.map(option => `
        <option value="${escapeHtml(option.value)}" ${option.value === selected ? "selected" : ""}>
            ${escapeHtml(option.label)}
        </option>
    `).join("");
}

function notice(message, error = false) {
    return message ? `<div class="notice ${error ? "error" : ""}">${escapeHtml(message)}</div>` : "";
}

function tableEmpty(colspan, text) {
    return `<tr><td colspan="${colspan}">${escapeHtml(text)}</td></tr>`;
}

function questionOptions(question) {
    const options = [
        ["A", question.optionA],
        ["B", question.optionB],
        ["C", question.optionC],
        ["D", question.optionD]
    ].filter(([, text]) => text);
    return options.map(([key, text]) => `<p>${key}. ${escapeHtml(text)}</p>`).join("");
}

async function init() {
    meta = await api("/meta");
    try {
        currentUser = await api("/auth/me");
        if (sessionStorage.getItem(TAB_AUTH_KEY) !== "true") {
            await api("/auth/logout", {method: "POST"});
            currentUser = null;
            location.hash = "";
            renderLogin();
            return;
        }
        renderShell();
    } catch {
        renderLogin();
    }
}

function renderLogin(message = "", registerMode = false) {
    clearTimer();
    const activeRole = localStorage.getItem("exam.activeRole") || "STUDENT";
    const canRegister = activeRole === "STUDENT";
    app.innerHTML = `
        <main class="login-wrap">
            <div class="login-stage">
                <div class="login-title">
                    <h1>在线考试系统</h1>
                    <p>ONLINE EXAM PLATFORM</p>
                </div>
                <section class="login-box">
                    <div id="loginNotice">${notice(message, Boolean(message))}</div>
                    <nav class="tabs">
                        ${meta.roles.map(role => `
                            <button type="button" data-role="${role.value}" class="${role.value === activeRole ? "active" : ""}">
                                ${escapeHtml(role.label)}登录
                            </button>
                        `).join("")}
                    </nav>
                    ${registerMode ? `<h2 class="form-heading">注册账号</h2>` : ""}
                    ${registerMode ? registerFormTemplate() : loginFormTemplate(activeRole, canRegister)}
                </section>
            </div>
        </main>
    `;
    document.querySelectorAll(".tabs button").forEach(button => {
        button.addEventListener("click", () => {
            localStorage.setItem("exam.activeRole", button.dataset.role);
            renderLogin();
        });
    });
    if (registerMode) {
        bindRegisterForm();
        return;
    }
    document.getElementById("loginForm").addEventListener("submit", async event => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        try {
            currentUser = await api("/auth/login", {
                method: "POST",
                body: JSON.stringify(Object.fromEntries(form.entries()))
            });
            sessionStorage.setItem(TAB_AUTH_KEY, "true");
            setHash(roleHome[currentUser.role]);
            renderShell();
        } catch (error) {
            document.getElementById("loginNotice").innerHTML = notice(error.message, true);
        }
    });
    document.getElementById("showRegisterButton")?.addEventListener("click", () => renderLogin("", true));
}

function loginFormTemplate(activeRole, canRegister) {
    return `
        <form id="loginForm">
            <input type="hidden" name="role" value="${activeRole}">
            <div class="field">
                <label class="label" for="username">账号</label>
                <input id="username" name="username" placeholder="请输入账号" required>
            </div>
            <div class="field">
                <label class="label" for="password">密码</label>
                <input id="password" name="password" type="password" placeholder="请输入密码" required>
            </div>
            <button class="primary" style="width:100%" type="submit">立即登录</button>
        </form>
        ${canRegister ? `
            <div class="login-extra">
                <span>还没有学生账号？</span>
                <button class="ghost link-button" id="showRegisterButton" type="button">注册账号</button>
            </div>
        ` : ""}
    `;
}

function registerFormTemplate() {
    return `
        <form id="registerForm">
            <div class="field">
                <label class="label" for="registerUsername">学号</label>
                <input id="registerUsername" name="username" inputmode="numeric" pattern="\\d{10}"
                       maxlength="10" placeholder="请输入 10 位数字学号" required>
            </div>
            <div class="field">
                <label class="label" for="registerDisplayName">姓名</label>
                <input id="registerDisplayName" name="displayName" placeholder="请输入姓名" required>
            </div>
            <div class="field">
                <label class="label" for="registerPassword">密码</label>
                <input id="registerPassword" name="password" type="password" pattern="[A-Za-z0-9]{6,}"
                       minlength="6" placeholder="至少 6 位数字或字母" required>
            </div>
            <button class="primary" style="width:100%" type="submit">注册并进入学生端</button>
        </form>
        <div class="login-extra">
            <span>已有账号？</span>
            <button class="ghost link-button" id="showLoginButton" type="button">返回登录</button>
        </div>
    `;
}

function bindRegisterForm() {
    localStorage.setItem("exam.activeRole", "STUDENT");
    document.getElementById("showLoginButton").addEventListener("click", () => renderLogin());
    document.getElementById("registerForm").addEventListener("submit", async event => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const payload = Object.fromEntries(form.entries());
        try {
            currentUser = await api("/auth/register", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            sessionStorage.setItem(TAB_AUTH_KEY, "true");
            setHash(roleHome[currentUser.role]);
            renderShell();
        } catch (error) {
            document.getElementById("loginNotice").innerHTML = notice(error.message, true);
        }
    });
}

function renderShell() {
    clearTimer();
    if (!location.hash || !location.hash.startsWith(roleHome[currentUser.role])) {
        location.hash = roleHome[currentUser.role];
    }
    app.innerHTML = `
        <header class="topbar">
            <div class="brand">${roleBrand[currentUser.role]}</div>
            <nav class="nav" id="mainNav">${navLinks()}</nav>
            <div class="identity">
                <span>${escapeHtml(currentUser.displayName)}</span>
                <div class="avatar">${roleInitial[currentUser.role]}</div>
                <button class="ghost" id="logoutButton" type="button">退出</button>
            </div>
        </header>
        <main class="shell" id="view"></main>
    `;
    document.getElementById("logoutButton").addEventListener("click", async () => {
        await api("/auth/logout", {method: "POST"});
        sessionStorage.removeItem(TAB_AUTH_KEY);
        currentUser = null;
        location.hash = "";
        renderLogin();
    });
    renderRoute();
}

function navLinks() {
    if (currentUser.role === "TEACHER") {
        return `
            <a href="#/teacher" data-route="#/teacher">仪表盘</a>
            <a href="#/teacher/exams" data-route="#/teacher/exams">试卷管理</a>
            <a href="#/teacher/questions" data-route="#/teacher/questions">题库中心</a>
        `;
    }
    if (currentUser.role === "ADMIN") {
        return `<a href="#/admin" data-route="#/admin">仪表盘</a>`;
    }
    return `<a href="#/student" data-route="#/student">考试</a>`;
}

function updateNav() {
    document.querySelectorAll("#mainNav a").forEach(link => {
        const route = link.dataset.route;
        const exact = location.hash === route;
        const child = route !== roleHome[currentUser.role] && location.hash.startsWith(route);
        link.classList.toggle("active", exact || child);
    });
}

async function renderRoute() {
    if (!currentUser) {
        return;
    }
    clearTimer();
    updateNav();
    const route = location.hash.slice(1);
    try {
        if (currentUser.role === "STUDENT") {
            await renderStudent(route);
        } else if (currentUser.role === "TEACHER") {
            await renderTeacher(route);
        } else {
            await renderAdmin();
        }
    } catch (error) {
        document.getElementById("view").innerHTML = notice(error.message, true);
    }
}

async function renderStudent(route) {
    const parts = route.split("/");
    if (parts[2] === "exam") {
        return renderTakeExam(parts[3]);
    }
    if (parts[2] === "result") {
        return renderResult(parts[3]);
    }
    const data = await api("/student/dashboard");
    const submitted = data.submittedResultIds || {};
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>可参加考试</h1><p>进入考试后按题型作答，客观题自动评分，大题由教师阅卷。</p></div>
        </section>
        <div class="grid stats">
            <div class="stat"><span>开放考试</span><strong>${data.exams.length}</strong></div>
            <div class="stat"><span>提交记录</span><strong>${data.submissions.length}</strong></div>
            <div class="stat"><span>学习状态</span><strong style="font-size:30px">READY</strong></div>
        </div>
        <div class="table-wrap"><table class="table">
            <thead><tr><th>考试名称</th><th>时长</th><th>题量</th><th>总分</th><th>操作</th></tr></thead>
            <tbody>
                ${data.exams.map(exam => {
                    const resultId = submitted[exam.id];
                    return `<tr>
                        <td><strong>${escapeHtml(exam.title)}</strong><p>${escapeHtml(exam.description)}</p></td>
                        <td>${exam.durationMinutes} 分钟</td>
                        <td>${exam.questionCount}</td>
                        <td>${exam.totalScore}</td>
                        <td>${resultId
                            ? `<button data-go="#/student/result/${resultId}">查看结果</button>`
                            : `<button class="primary" data-go="#/student/exam/${exam.id}">进入考试</button>`}
                        </td>
                    </tr>`;
                }).join("") || tableEmpty(5, "暂无开放考试。")}
            </tbody>
        </table></div>
        <section class="section">
            <h2>我的成绩</h2>
            <div class="table-wrap"><table class="table">
                <thead><tr><th>考试</th><th>提交时间</th><th>成绩</th><th>状态</th><th>详情</th></tr></thead>
                <tbody>
                    ${data.submissions.map(submission => `<tr>
                        <td>${escapeHtml(submission.exam.title)}</td>
                        <td>${formatTime(submission.submittedAt)}</td>
                        <td>${submission.totalScore} / ${submission.exam.totalScore}</td>
                        <td><span class="badge">${escapeHtml(submission.statusLabel)}</span></td>
                        <td><button data-go="#/student/result/${submission.id}">查看</button></td>
                    </tr>`).join("") || tableEmpty(5, "还没有交卷记录。")}
                </tbody>
            </table></div>
        </section>
    `;
    bindGoButtons();
}

async function renderTakeExam(id) {
    const exam = await api(`/student/exams/${id}`);
    document.getElementById("view").classList.add("narrow");
    document.getElementById("view").innerHTML = `
        <section>
            <h1>${escapeHtml(exam.title)}</h1>
            <p>${escapeHtml(exam.description)}</p>
            <div class="exam-meta">
                <strong>时长：${exam.durationMinutes} 分钟</strong>
                <strong>题量：${exam.questions.length}</strong>
                <strong>总分：${exam.totalScore}</strong>
            </div>
        </section>
        <form id="examForm">
            <div class="notice sticky-timer">
                <span>剩余时间</span>
                <strong id="countdown">--:--</strong>
            </div>
            ${exam.questions.map((question, index) => renderQuestionInput(question, index)).join("")}
            <div class="actions" style="justify-content:space-between">
                <button type="button" data-go="#/student">返回</button>
                <button id="submitButton" class="primary" type="submit">提交试卷</button>
            </div>
        </form>
    `;
    bindGoButtons();
    startExamTimer(exam.durationMinutes);
    document.getElementById("examForm").addEventListener("submit", async event => {
        event.preventDefault();
        const submitButton = document.getElementById("submitButton");
        submitButton.disabled = true;
        submitButton.textContent = "正在提交...";
        const form = new FormData(event.currentTarget);
        const answers = {};
        for (const [key, value] of form.entries()) {
            answers[key.replace("question_", "")] = value;
        }
        try {
            const submission = await api(`/student/exams/${id}/submissions`, {
                method: "POST",
                body: JSON.stringify({answers})
            });
            setHash(`#/student/result/${submission.id}`);
        } catch (error) {
            submitButton.disabled = false;
            submitButton.textContent = "提交试卷";
            document.getElementById("view").insertAdjacentHTML("afterbegin", notice(error.message, true));
        }
    });
}

function renderQuestionInput(question, index) {
    const name = `question_${question.id}`;
    let input = "";
    if (question.type === "SINGLE_CHOICE") {
        input = [["A", question.optionA], ["B", question.optionB], ["C", question.optionC], ["D", question.optionD]]
            .filter(([, text]) => text)
            .map(([key, text], optionIndex) => `
                <label class="option">
                    <input type="radio" name="${name}" value="${key}" ${optionIndex === 0 ? "required" : ""}>
                    <span>${key}. ${escapeHtml(text)}</span>
                </label>
            `).join("");
    } else if (question.type === "TRUE_FALSE") {
        input = `
            <label class="option"><input type="radio" name="${name}" value="正确" required><span>正确</span></label>
            <label class="option"><input type="radio" name="${name}" value="错误"><span>错误</span></label>
        `;
    } else if (question.type === "FILL_BLANK") {
        input = `<input name="${name}" placeholder="请输入答案" required>`;
    } else {
        input = `<textarea name="${name}" placeholder="请输入完整作答" required></textarea>`;
    }
    return `
        <article class="question-block">
            <div class="actions" style="justify-content:space-between;margin-bottom:18px">
                <h3>第 ${index + 1} 题 · ${escapeHtml(question.typeLabel)}</h3>
                <span class="badge">${question.score} 分</span>
            </div>
            <p class="question-title">${escapeHtml(question.title)}</p>
            ${input}
        </article>
    `;
}

function startExamTimer(minutes) {
    const countdown = document.getElementById("countdown");
    let remaining = Number(minutes || 0) * 60;
    const render = () => {
        const mins = Math.floor(remaining / 60).toString().padStart(2, "0");
        const secs = Math.max(remaining % 60, 0).toString().padStart(2, "0");
        countdown.textContent = `${mins}:${secs}`;
    };
    render();
    timerId = setInterval(() => {
        remaining -= 1;
        render();
        if (remaining <= 0) {
            clearTimer();
            document.getElementById("examForm").requestSubmit();
        }
    }, 1000);
}

async function renderResult(id) {
    const submission = await api(`/student/submissions/${id}`);
    document.getElementById("view").classList.add("narrow");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>考试结果</h1><p>${escapeHtml(submission.exam.title)}</p></div>
            <button data-go="#/student">返回考试中心</button>
        </section>
        <div class="grid stats">
            <div class="stat"><span>客观题</span><strong>${submission.objectiveScore}</strong></div>
            <div class="stat"><span>主观题</span><strong>${submission.subjectiveScore}</strong></div>
            <div class="stat"><span>总成绩</span><strong>${submission.totalScore}/${submission.exam.totalScore}</strong></div>
        </div>
        ${submission.answers.map((answer, index) => `
            <article class="question-block">
                <div class="actions" style="justify-content:space-between;margin-bottom:18px">
                    <h3>第 ${index + 1} 题 · ${escapeHtml(answer.question.typeLabel)}</h3>
                    <span class="badge">${answer.score ?? "待评分"} / ${answer.question.score}</span>
                </div>
                <p class="question-title">${escapeHtml(answer.question.title)}</p>
                <p>你的答案：<strong>${escapeHtml(answer.answerText)}</strong></p>
                ${answer.question.correctAnswer ? `<p>标准答案：<strong>${escapeHtml(answer.question.correctAnswer)}</strong></p>` : ""}
                ${answer.teacherComment ? `<p>教师评语：<strong>${escapeHtml(answer.teacherComment)}</strong></p>` : ""}
            </article>
        `).join("")}
    `;
    bindGoButtons();
}

async function renderTeacher(route) {
    document.getElementById("view").classList.remove("narrow");
    const parts = route.split("/");
    if (parts[2] === "questions") {
        return renderTeacherQuestions();
    }
    if (parts[2] === "exams" && parts[3] === "new") {
        return renderNewExam();
    }
    if (parts[2] === "exams" && parts[4] === "submissions") {
        return renderTeacherSubmissions(parts[3]);
    }
    if (parts[2] === "exams") {
        return renderTeacherExams();
    }
    if (parts[2] === "submissions" && parts[4] === "grade") {
        return renderGrade(parts[3]);
    }
    const data = await api("/teacher/dashboard");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>仪表盘概览</h1><p>管理试卷、上传题目、跟进提交与阅卷状态。</p></div>
            <button class="primary" data-go="#/teacher/exams/new">新建考试</button>
        </section>
        <div class="grid stats">
            <div class="stat"><span>我的试卷</span><strong>${data.exams.length}</strong></div>
            <div class="stat"><span>题库题目</span><strong>${data.questionCount}</strong></div>
            <div class="stat"><span>答卷记录</span><strong>${data.submissionCount}</strong></div>
        </div>
        ${examTable(data.exams, true)}
    `;
    bindGoButtons();
}

async function renderTeacherQuestions(message = "") {
    const questions = await api("/teacher/questions");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>题库中心</h1><p>支持选择题、判断题、填空题和大题上传，也可以按格式批量导入。</p></div>
            <button data-go="#/teacher/exams/new">组卷</button>
        </section>
        ${notice(message)}
        <div class="grid two">
            <section class="panel">
                <h2>上传单题</h2>
                <form id="questionForm">
                    <div class="field"><label class="label">题型</label><select name="type">${optionList(meta.questionTypes)}</select></div>
                    <div class="field"><label class="label">题干</label><textarea name="title" required></textarea></div>
                    <div class="grid two">
                        <div class="field"><label class="label">选项 A</label><input name="optionA"></div>
                        <div class="field"><label class="label">选项 B</label><input name="optionB"></div>
                        <div class="field"><label class="label">选项 C</label><input name="optionC"></div>
                        <div class="field"><label class="label">选项 D</label><input name="optionD"></div>
                    </div>
                    <div class="field"><label class="label">标准答案</label><input name="correctAnswer" required></div>
                    <div class="field"><label class="label">分值</label><input name="score" type="number" min="1" value="10" required></div>
                    <button class="primary" type="submit">上传题目</button>
                </form>
            </section>
            <section class="panel">
                <h2>从本机上传题目文件</h2>
                <form id="fileImportForm">
                    <div class="field">
                        <label class="label">题目文件</label>
                        <input id="questionFile" type="file" accept=".txt,.csv,text/plain,text/csv">
                    </div>
                    <div class="actions" style="margin-bottom:22px">
                        <button class="accent" type="submit">上传并导入</button>
                        <button type="button" id="downloadTemplateButton">下载模板</button>
                    </div>
                </form>
                <h2>批量粘贴</h2>
                <form id="importForm">
                    <div class="field">
                        <label class="label">题目文本</label>
                        <textarea name="payload" style="min-height:308px" placeholder="选择题|JVM 的作用是什么？|运行 Java 字节码|管理数据库|编写页面|发送邮件|A|10"></textarea>
                    </div>
                    <button type="submit">导入题目</button>
                </form>
            </section>
        </div>
        <section class="section">
            <div class="table-wrap"><table class="table">
                <thead><tr><th>题型</th><th>题干</th><th>答案</th><th>分值</th><th>操作</th></tr></thead>
                <tbody>
                    ${questions.map(question => `<tr>
                        <td><span class="badge">${escapeHtml(question.typeLabel)}</span></td>
                        <td><strong>${escapeHtml(question.title)}</strong>${questionOptions(question)}</td>
                        <td>${escapeHtml(question.correctAnswer)}</td>
                        <td>${question.score}</td>
                        <td><button data-delete-question="${question.id}">删除</button></td>
                    </tr>`).join("") || tableEmpty(5, "题库暂无题目。")}
                </tbody>
            </table></div>
        </section>
    `;
    bindGoButtons();
    document.getElementById("questionForm").addEventListener("submit", submitQuestion);
    document.getElementById("importForm").addEventListener("submit", importQuestions);
    document.getElementById("fileImportForm").addEventListener("submit", importQuestionsFromFile);
    document.getElementById("downloadTemplateButton").addEventListener("click", downloadQuestionTemplate);
    document.querySelectorAll("[data-delete-question]").forEach(button => {
        button.addEventListener("click", async () => {
            if (!confirm("确认删除这道题目？")) {
                return;
            }
            await api(`/teacher/questions/${button.dataset.deleteQuestion}`, {method: "DELETE"});
            renderTeacherQuestions("题目已删除");
        });
    });
}

async function submitQuestion(event) {
    event.preventDefault();
    const form = Object.fromEntries(new FormData(event.currentTarget).entries());
    form.score = Number(form.score);
    try {
        await api("/teacher/questions", {method: "POST", body: JSON.stringify(form)});
        renderTeacherQuestions("题目已上传到题库");
    } catch (error) {
        renderTeacherQuestions(error.message);
    }
}

async function importQuestions(event) {
    event.preventDefault();
    try {
        const result = await api("/teacher/questions/import", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget).entries()))
        });
        renderTeacherQuestions(`批量上传完成，成功导入 ${result.imported} 道题`);
    } catch (error) {
        renderTeacherQuestions(error.message);
    }
}

async function importQuestionsFromFile(event) {
    event.preventDefault();
    const file = document.getElementById("questionFile").files[0];
    if (!file) {
        renderTeacherQuestions("请选择一个 .txt 或 .csv 题目文件");
        return;
    }
    try {
        const payload = await file.text();
        const result = await api("/teacher/questions/import", {
            method: "POST",
            body: JSON.stringify({payload})
        });
        renderTeacherQuestions(`文件导入完成，成功导入 ${result.imported} 道题`);
    } catch (error) {
        renderTeacherQuestions(error.message);
    }
}

function downloadQuestionTemplate() {
    const content = [
        "题型|题干|A|B|C|D|答案|分值",
        "选择题|JVM 的作用是什么？|运行 Java 字节码|管理数据库|编写页面|发送邮件|A|10",
        "判断题|HTTP 是无状态协议。|||||正确|10",
        "填空题|Spring Boot 默认内嵌容器是 ____。|||||Tomcat|10",
        "大题|说明如何防止重复提交。|||||人工评分|20"
    ].join("\\n");
    const blob = new Blob([content], {type: "text/plain;charset=utf-8"});
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "question-import-template.txt";
    link.click();
    URL.revokeObjectURL(url);
}

async function renderTeacherExams() {
    const exams = await api("/teacher/exams");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>试卷管理</h1><p>创建、发布、关闭考试并查看学生提交记录。</p></div>
            <button class="primary" data-go="#/teacher/exams/new">新建考试</button>
        </section>
        ${examTable(exams, false)}
    `;
    bindGoButtons();
    document.querySelectorAll("[data-status]").forEach(button => {
        button.addEventListener("click", async () => {
            await api(`/teacher/exams/${button.dataset.exam}/status`, {
                method: "PATCH",
                body: JSON.stringify({status: button.dataset.status})
            });
            renderTeacherExams();
        });
    });
}

function examTable(exams, compact) {
    return `<div class="table-wrap"><table class="table">
        <thead><tr><th>试卷名称</th><th>时长</th><th>题量</th><th>总分</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
            ${exams.map(exam => `<tr>
                <td><strong>${escapeHtml(exam.title)}</strong><p>${escapeHtml(exam.description)}</p></td>
                <td>${exam.durationMinutes} 分钟</td>
                <td>${exam.questionCount}</td>
                <td>${exam.totalScore}</td>
                <td><span class="badge ${exam.status === "PUBLISHED" ? "dark" : ""}">${escapeHtml(exam.statusLabel)}</span></td>
                <td><div class="actions">
                    ${compact ? "" : `
                        <button data-exam="${exam.id}" data-status="PUBLISHED">发布</button>
                        <button data-exam="${exam.id}" data-status="CLOSED">关闭</button>
                    `}
                    <button data-go="#/teacher/exams/${exam.id}/submissions">提交记录</button>
                </div></td>
            </tr>`).join("") || tableEmpty(6, "暂无试卷，请先创建考试。")}
        </tbody>
    </table></div>`;
}

async function renderNewExam() {
    const questions = await api("/teacher/questions");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>新建考试</h1><p>从题库中选择题目组成试卷，创建后可发布给学生。</p></div>
            <button data-go="#/teacher/questions">上传题目</button>
        </section>
        <form id="examCreateForm">
            <section class="panel">
                <div class="grid two">
                    <div class="field"><label class="label">考试名称</label><input name="title" required></div>
                    <div class="field"><label class="label">考试时长（分钟）</label><input name="durationMinutes" type="number" min="10" value="60" required></div>
                </div>
                <div class="field"><label class="label">考试说明</label><textarea name="description"></textarea></div>
            </section>
            <section class="section">
                <h2>选择题目</h2>
                <div class="table-wrap"><table class="table">
                    <thead><tr><th>选择</th><th>题型</th><th>题干</th><th>分值</th></tr></thead>
                    <tbody>
                        ${questions.map(question => `<tr>
                            <td><label class="pick"><input type="checkbox" name="questionIds" value="${question.id}"></label></td>
                            <td><span class="badge">${escapeHtml(question.typeLabel)}</span></td>
                            <td>${escapeHtml(question.title)}</td>
                            <td>${question.score}</td>
                        </tr>`).join("") || tableEmpty(4, "题库为空，请先上传题目。")}
                    </tbody>
                </table></div>
            </section>
            <div class="actions section">
                <button class="primary" type="submit">创建试卷</button>
                <button type="button" data-go="#/teacher/exams">返回列表</button>
            </div>
        </form>
    `;
    bindGoButtons();
    document.getElementById("examCreateForm").addEventListener("submit", async event => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const payload = {
            title: form.get("title"),
            description: form.get("description"),
            durationMinutes: Number(form.get("durationMinutes")),
            questionIds: form.getAll("questionIds").map(Number)
        };
        await api("/teacher/exams", {method: "POST", body: JSON.stringify(payload)});
        setHash("#/teacher/exams");
    });
}

async function renderTeacherSubmissions(examId) {
    const [exam, submissions] = await Promise.all([
        api(`/teacher/exams/${examId}`),
        api(`/teacher/exams/${examId}/submissions`)
    ]);
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>提交记录</h1><p>${escapeHtml(exam.title)}</p></div>
            <button data-go="#/teacher/exams">返回试卷</button>
        </section>
        <div class="table-wrap"><table class="table">
            <thead><tr><th>学生</th><th>提交时间</th><th>客观题</th><th>总分</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
                ${submissions.map(submission => `<tr>
                    <td>${escapeHtml(submission.student.displayName)}</td>
                    <td>${formatTime(submission.submittedAt)}</td>
                    <td>${submission.objectiveScore}</td>
                    <td>${submission.totalScore} / ${submission.exam.totalScore}</td>
                    <td><span class="badge ${submission.status === "GRADED" ? "dark" : ""}">${escapeHtml(submission.statusLabel)}</span></td>
                    <td><button data-go="#/teacher/submissions/${submission.id}/grade">阅卷</button></td>
                </tr>`).join("") || tableEmpty(6, "暂无学生提交。")}
            </tbody>
        </table></div>
    `;
    bindGoButtons();
}

async function renderGrade(id) {
    const submission = await api(`/teacher/submissions/${id}`);
    document.getElementById("view").classList.add("narrow");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>教师阅卷</h1><p>${escapeHtml(submission.exam.title)} · ${escapeHtml(submission.student.displayName)}</p></div>
            <button data-go="#/teacher/exams/${submission.exam.id}/submissions">返回记录</button>
        </section>
        <form id="gradeForm">
            ${submission.answers.map((answer, index) => `
                <article class="question-block">
                    <div class="actions" style="justify-content:space-between;margin-bottom:18px">
                        <h3>第 ${index + 1} 题 · ${escapeHtml(answer.question.typeLabel)}</h3>
                        <span class="badge">${answer.question.score} 分</span>
                    </div>
                    <p class="question-title">${escapeHtml(answer.question.title)}</p>
                    <p>学生答案</p>
                    <div class="panel" style="margin:10px 0 18px;padding:18px">${escapeHtml(answer.answerText)}</div>
                    ${answer.question.type !== "ESSAY" ? `
                        <p>标准答案：<strong>${escapeHtml(answer.question.correctAnswer)}</strong></p>
                        <p>系统判分：<strong>${answer.score ?? 0}</strong></p>
                    ` : `
                        <div class="grid two">
                            <div class="field">
                                <label class="label">教师评分</label>
                                <input data-score="${answer.id}" type="number" min="0" max="${answer.question.score}" value="${answer.score ?? 0}">
                            </div>
                            <div class="field">
                                <label class="label">评语</label>
                                <input data-comment="${answer.id}" value="${escapeHtml(answer.teacherComment)}">
                            </div>
                        </div>
                    `}
                </article>
            `).join("")}
            <div class="actions">
                <button class="primary" type="submit">保存阅卷结果</button>
                <span class="badge dark">当前总分 ${submission.totalScore} / ${submission.exam.totalScore}</span>
            </div>
        </form>
    `;
    bindGoButtons();
    document.getElementById("gradeForm").addEventListener("submit", async event => {
        event.preventDefault();
        const scores = {};
        const comments = {};
        document.querySelectorAll("[data-score]").forEach(input => scores[input.dataset.score] = Number(input.value));
        document.querySelectorAll("[data-comment]").forEach(input => comments[input.dataset.comment] = input.value);
        await api(`/teacher/submissions/${id}/grade`, {
            method: "POST",
            body: JSON.stringify({scores, comments})
        });
        renderGrade(id);
    });
}

async function renderAdmin() {
    document.getElementById("view").classList.remove("narrow");
    const data = await api("/admin/overview");
    document.getElementById("view").innerHTML = `
        <section class="page-head">
            <div><h1>运行概览</h1><p>查看用户规模、开放考试和待阅卷数量，维护系统账号。</p></div>
        </section>
        <div class="grid stats">
            <div class="stat"><span>总用户数</span><strong>${data.totalUsers}</strong></div>
            <div class="stat"><span>开放考试</span><strong>${data.publishedExamCount}</strong></div>
            <div class="stat"><span>待阅卷</span><strong>${data.pendingGradeCount}</strong></div>
        </div>
        <div class="grid two">
            <section class="panel">
                <h2>创建用户</h2>
                <form id="userForm">
                    <div class="field"><label class="label">用户名</label><input name="username" required></div>
                    <div class="field"><label class="label">显示名称</label><input name="displayName" required></div>
                    <div class="field"><label class="label">密码</label><input name="password" type="password" minlength="6" required></div>
                    <div class="field"><label class="label">身份</label><select name="role">${optionList(meta.roles)}</select></div>
                    <button class="primary" type="submit">创建账号</button>
                </form>
            </section>
            <section class="panel">
                <h2>角色分布</h2>
                <p>学生账号：<strong>${data.studentCount}</strong></p>
                <p>教师账号：<strong>${data.teacherCount}</strong></p>
                <p>生产环境可通过环境变量切换到 MySQL，并配置前端域名的 CORS 白名单。</p>
            </section>
        </div>
        <section class="section">
            <h2>用户列表</h2>
            <div class="table-wrap"><table class="table">
                <thead><tr><th>ID</th><th>用户名</th><th>显示名称</th><th>身份</th><th>状态</th><th>操作</th></tr></thead>
                <tbody>
                    ${data.users.map(user => `<tr>
                        <td>${user.id}</td>
                        <td>${escapeHtml(user.username)}</td>
                        <td>${escapeHtml(user.displayName)}</td>
                        <td><span class="badge">${escapeHtml(user.roleLabel)}</span></td>
                        <td><span class="badge ${user.enabled ? "dark" : ""}">${user.enabled ? "启用" : "停用"}</span></td>
                        <td><div class="actions compact">
                            <button data-user-enabled="${user.id}" data-enabled="${!user.enabled}">${user.enabled ? "停用" : "启用"}</button>
                            <input data-password="${user.id}" type="password" minlength="6" placeholder="新密码">
                            <button data-reset="${user.id}">重置</button>
                        </div></td>
                    </tr>`).join("")}
                </tbody>
            </table></div>
        </section>
        <section class="section">
            <h2>考试列表</h2>
            <div class="table-wrap"><table class="table">
                <thead><tr><th>名称</th><th>教师</th><th>题量</th><th>总分</th><th>状态</th></tr></thead>
                <tbody>
                    ${data.exams.map(exam => `<tr>
                        <td>${escapeHtml(exam.title)}</td>
                        <td>${escapeHtml(exam.teacherName)}</td>
                        <td>${exam.questionCount}</td>
                        <td>${exam.totalScore}</td>
                        <td><span class="badge">${escapeHtml(exam.statusLabel)}</span></td>
                    </tr>`).join("") || tableEmpty(5, "暂无考试。")}
                </tbody>
            </table></div>
        </section>
    `;
    document.getElementById("userForm").addEventListener("submit", async event => {
        event.preventDefault();
        await api("/admin/users", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(new FormData(event.currentTarget).entries()))
        });
        renderAdmin();
    });
    document.querySelectorAll("[data-user-enabled]").forEach(button => {
        button.addEventListener("click", async () => {
            await api(`/admin/users/${button.dataset.userEnabled}/enabled`, {
                method: "PATCH",
                body: JSON.stringify({enabled: button.dataset.enabled === "true"})
            });
            renderAdmin();
        });
    });
    document.querySelectorAll("[data-reset]").forEach(button => {
        button.addEventListener("click", async () => {
            const input = document.querySelector(`[data-password="${button.dataset.reset}"]`);
            await api(`/admin/users/${button.dataset.reset}/password`, {
                method: "PATCH",
                body: JSON.stringify({newPassword: input.value})
            });
            renderAdmin();
        });
    });
}

function bindGoButtons() {
    document.querySelectorAll("[data-go]").forEach(button => {
        button.addEventListener("click", () => setHash(button.dataset.go));
    });
}

function clearTimer() {
    if (timerId) {
        clearInterval(timerId);
        timerId = null;
    }
    const view = document.getElementById("view");
    if (view) {
        view.classList.remove("narrow");
    }
}

window.addEventListener("hashchange", () => {
    if (currentUser) {
        renderRoute();
    }
});

init().catch(error => {
    app.innerHTML = `<main class="shell">${notice(error.message, true)}</main>`;
});
