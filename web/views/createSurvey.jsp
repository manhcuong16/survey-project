<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <title>Create Survey</title>
        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/static/css/style.css">
    </head>

    <body>
        <div class="form-container">
            <h2>Create Survey</h2>

            <form action="${pageContext.request.contextPath}/create" method="post">
                <input type="hidden" name="questionCount" id="questionCount" value="0">
                <input type="hidden" name="questionIds" id="questionIds" value="">

                <div class="question-box meta-box">
                    <h4>Survey Info</h4>

                    Title<br>
                    <input type="text" name="title" required>

                    <br><br>

                    Description<br>
                    <textarea name="description" placeholder="Description..."></textarea>
                </div>

                <br><br>

                <div id="question-container"></div>

                <button class="add-btn" type="button" onclick="addQuestion()">
                    Add Question
                </button>

                <br><br>

                <button class="submit-btn" type="submit">
                    Create Survey
                </button>
            </form>
        </div>

        <script>
            let questionCount = 0;

            // Keep the current values so switching MCQ <-> TEXT does not wipe what you typed.
            const questionState = {};
            const activeQuestionIds = [];

            function setQuestionCountInput(value) {
                const input = document.getElementById("questionCount");
                if (input) {
                    input.value = String(value);
                }
            }

            function syncQuestionIdsInput() {
                const input = document.getElementById("questionIds");
                if (input) {
                    input.value = activeQuestionIds.join(",");
                }
            }

            function renumberQuestionHeaders() {
                const boxes = document.querySelectorAll("#question-container .question-box");
                boxes.forEach((box, idx) => {
                    const title = box.querySelector("h4[data-question-title=\"1\"]");
                    if (title) {
                        title.textContent = "Question " + (idx + 1);
                    }
                });
            }

            function getQuestionState(id) {
                if (!questionState[id]) {
                    questionState[id] = {
                        type: "MCQ",
                        options: ["", "", "", ""],
                        text: ""
                    };
                }
                return questionState[id];
            }

            function persistState(id) {
                const state = getQuestionState(id);

                if (state.type === "MCQ") {
                    const optionInputs = document.querySelectorAll(
                            "#options_" + id + " input[data-option-input=\"1\"]"
                            );
                    state.options = Array.from(optionInputs).map(i => i.value);
                    return;
                }

                if (state.type === "TEXT") {
                    const textInput = document.querySelector(
                            "#answer_" + id + " input[data-text-answer=\"1\"]"
                            );
                    state.text = textInput ? textInput.value : "";
                }
            }

            function createOptionRow(questionId, optionIndex, value, canDelete) {
                const label = document.createElement("label");
                label.className = "option";

                const radio = document.createElement("input");
                radio.type = "radio";
                radio.disabled = true;

                const input = document.createElement("input");
                input.type = "text";
                input.name = "option_" + questionId + "_" + optionIndex;
                input.placeholder = "Option " + optionIndex;
                input.value = value || "";
                input.setAttribute("data-option-input", "1");

                label.appendChild(radio);
                label.appendChild(input);

                if (canDelete) {
                    const delBtn = document.createElement("button");
                    delBtn.type = "button";
                    delBtn.className = "delete-option-btn";
                    delBtn.textContent = "X";
                    delBtn.title = "Remove option";
                    delBtn.addEventListener("click", () => removeOption(questionId, optionIndex));
                    label.appendChild(delBtn);
                }

                return label;
            }

            function renderMcq(questionId) {
                const state = getQuestionState(questionId);
                const answerDiv = document.getElementById("answer_" + questionId);

                answerDiv.innerHTML = "";

                const optionsContainer = document.createElement("div");
                optionsContainer.id = "options_" + questionId;

                const options = (state.options && state.options.length) ? state.options : ["", ""];
                const canDelete = options.length > 1;
                options.forEach((value, index) => {
                    optionsContainer.appendChild(
                            createOptionRow(questionId, index + 1, value, canDelete)
                            );
                });

                const addBtn = document.createElement("button");
                addBtn.type = "button";
                addBtn.className = "add-option-btn";
                addBtn.textContent = "+ Add option";
                addBtn.addEventListener("click", () => addOption(questionId));

                answerDiv.appendChild(optionsContainer);
                answerDiv.appendChild(addBtn);
            }

            function renderText(questionId) {
                const state = getQuestionState(questionId);
                const answerDiv = document.getElementById("answer_" + questionId);

                answerDiv.innerHTML = "";

                const input = document.createElement("input");
                input.type = "text";
                input.name = "text_" + questionId;
                input.placeholder = "Text answer";
                input.value = state.text || "";
                input.setAttribute("data-text-answer", "1");

                answerDiv.appendChild(input);
            }

            function addOption(questionId) {
                const state = getQuestionState(questionId);

                // Read current values first, then append a new blank option.
                persistState(questionId);
                state.options.push("");

                // Re-render so names/placeholders stay contiguous and delete buttons update.
                renderMcq(questionId);
            }

            function removeOption(questionId, optionIndex) {
                const state = getQuestionState(questionId);
                if (state.type !== "MCQ") {
                    return;
                }

                persistState(questionId);

                if (!state.options || state.options.length <= 1) {
                    return;
                }

                state.options.splice(optionIndex - 1, 1);
                renderMcq(questionId);
            }

            function deleteQuestion(questionId) {
                const box = document.querySelector(
                        "#question-container .question-box[data-question-id=\"" + questionId + "\"]"
                        );
                if (box) {
                    box.remove();
                }

                delete questionState[questionId];

                const idx = activeQuestionIds.indexOf(questionId);
                if (idx >= 0) {
                    activeQuestionIds.splice(idx, 1);
                }

                syncQuestionIdsInput();
                renumberQuestionHeaders();
            }

            function changeType(select, questionId) {
                const state = getQuestionState(questionId);

                // Save current inputs before changing UI.
                persistState(questionId);

                state.type = select.value;

                if (state.type === "TEXT") {
                    renderText(questionId);
                } else {
                    renderMcq(questionId);
                }
            }

            function addQuestion() {
                questionCount++;
                setQuestionCountInput(questionCount);

                const id = questionCount;
                getQuestionState(id); // init

                activeQuestionIds.push(id);
                syncQuestionIdsInput();

                const questionBox = document.createElement("div");
                questionBox.className = "question-box";
                questionBox.setAttribute("data-question-id", String(id));

                const header = document.createElement("div");
                header.className = "question-header";

                const title = document.createElement("h4");
                title.setAttribute("data-question-title", "1");

                const deleteBtn = document.createElement("button");
                deleteBtn.type = "button";
                deleteBtn.className = "delete-btn";
                deleteBtn.textContent = "X";
                deleteBtn.title = "Delete question";
                deleteBtn.addEventListener("click", () => deleteQuestion(id));

                header.appendChild(title);
                header.appendChild(deleteBtn);

                const questionInput = document.createElement("input");
                questionInput.type = "text";
                questionInput.name = "question_" + id;
                questionInput.placeholder = "Enter question";
                questionInput.required = true;

                const typeSelect = document.createElement("select");
                typeSelect.name = "type_" + id;
                typeSelect.addEventListener("change", function () {
                    changeType(this, id);
                });

                const optionMcq = document.createElement("option");
                optionMcq.value = "MCQ";
                optionMcq.textContent = "Multiple Choice";

                const optionText = document.createElement("option");
                optionText.value = "TEXT";
                optionText.textContent = "Text Answer";

                typeSelect.appendChild(optionMcq);
                typeSelect.appendChild(optionText);

                const answerDiv = document.createElement("div");
                answerDiv.id = "answer_" + id;

                questionBox.appendChild(header);
                questionBox.appendChild(questionInput);
                questionBox.appendChild(document.createElement("br"));
                questionBox.appendChild(document.createElement("br"));
                questionBox.appendChild(typeSelect);
                questionBox.appendChild(document.createElement("br"));
                questionBox.appendChild(document.createElement("br"));
                questionBox.appendChild(answerDiv);
                questionBox.appendChild(document.createElement("hr"));

                document.getElementById("question-container").appendChild(questionBox);

                // Default UI: MCQ
                renderMcq(id);
                renumberQuestionHeaders();
            }
        </script>
    </body>
</html>