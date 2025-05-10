# Frontend Tasky Todos

## 1. Tasks Index

- [x] Define FSM for loading vs tasks
- [x] Render root tasks in table
- [x] Render subtasks in table
- [x] Update tasks-fsm to create instances of task-fsms
- [x] Separate task-fsm from new-task-fsm
- [x] Update task-row to not make all tasks editable
- [x] Clicking on task name should open sidebar or drill into it
- [ ] Add button to add nested new-task row
- [ ] Make footer form sticky
- [ ] Footer form should have a button to toggle the description field

## 2. Update Task

- [x] Define Task FSM
- [x] Support updating fields
- [x] After updating a field start a debounce timer to save
- [x] Send updates to server

## 3. Delete Task

- [x] Update Task FSM to support deleting
- [x] Draft alternative idea to annoying confirmation prompt modal
- [x] Refresh tasks
- [x] When deleted remove task-fsm from tasks-fsm
- [ ] Implement recursive soft-delete query function for setting deleted_at on subtasks

## 4. Drill-down into Task

- [x] Clicking on a task opens the task view
- [x] Update shadow-router to route all non-file, non-api routes to index.html
- [ ] Fetch all children of a subtask
- [x] Consider a history vector to show breadcrumbs

## 5. View Task

- [x] Remove estimated_time_map from backend
- [ ] Clicking on title should toggle edit input
- [ ] Pressing Esc while editing title should cancel edit input
- [ ] Clicking on description should toggle edit description
- [ ] Pressing Esc while editing description should cancel edit description

### 5.1 Time Sessions Table

- [ ] Render a row for each time session
- [ ] Format the started field as a date string
- [ ] Format the ended field as a time field if same date
- [ ] Format the elapsed in months, days, hours, minutes, and seconds
- [ ] Interrupted-by field should link to task

## 6. Clock in and out of a Task

- [ ] Render clock-in and clock-out buttons on right side of breadcrumbs
- [ ] Clock-in should create a time session with a started_at
- [ ] Clock-out should update a time session's ended at
- [ ] Clock-in on another task should update a session's ended_at + interrupted_by_task_id
- [ ] A permanent toast message should be displayed with a clock-out button
