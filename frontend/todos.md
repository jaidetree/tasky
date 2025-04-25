# Frontend Tasky Todos

## 1. Tasks Index

- [x] Define FSM for loading vs tasks
- [x] Render root tasks in table
- [x] Render subtasks in table
- [x] Update tasks-fsm to create instances of task-fsms
- [x] Separate task-fsm from new-task-fsm
- [x] Update task-row to not make all tasks editable
- [ ] Clicking on task name should open sidebar or drill into it
- [ ] Add button to add nested new-task row

## 2. Update Task

- [x] Define Task FSM
- [x] Support updating fields
- [x] After updating a field start a debounce timer to save
- [x] Send updates to server

## 3. Delete Task

- [x] Update Task FSM to support deleting
- [x] Draft alternative idea to annoying confirmation prompt modal
- [x] Refresh tasks
- [ ] When deleted remove task-fsm from tasks-fsm

## 4. Drill-down into Task

- [ ] Update shadow-router to route all non-file, non-api routes to index.html
- [ ] Fetch all children of a subtask
- [ ] Consider a history vector to show breadcrumbs
