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
- [x] Clicking on title should toggle edit input
- [x] Pressing Esc while editing title should cancel edit input
- [x] Clicking on description should toggle edit description
- [x] Pressing Esc while editing description should cancel edit description

### 5.1 Time Sessions Table

- [x] Render a row for each time session
- [x] Add clock-in buttons to the right of breadcrumbs
- [x] Update task-fsm to format time-sessions as {:order [] :all {}}
- [x] Update task-fsm to support clocked-in to assoc a new session
- [x] Update task-fsm to support update-session
- [x] Update task-fsm to support clock-in
- [x] Update task-fsm to support clock-out
- [x] Update task-fsm to support clocked-out
- [ ] Update task-fsm to support interrupt
- [x] Update task-fsm to support clocked-in state
- [x] Update task-fsm to support clocking-in state
- [x] Update task-fsm to support clocking-out state
- [ ] Update task-fsm to support interrupting state
- [x] Create fsm for managing the selected clocked-in task and handling interruption
- [ ] ~~Consider having a clocked-in effect that on dispose clocks out the task?~~
- [ ] Make the started and end dates formatting more user friendly
- [x] Make the elapsed and estimate format more user friendly
- [ ] When a session is clocked-in, show elapsed time counter
- [ ] Update time_session model to have a soft delete field
- [ ] Show deleted time_sessions crossed out and greyed out
- [ ] In the timer toast, show total elapsed time of all sessions against the estimate
- [x] Update backend to sort time sessions by start_time DESCENDING

### 5.2 Clock in and out of a Task

- [x] Render clock-in and clock-out buttons on right side of breadcrumbs
- [ ] Update fsm to store session in target task-fsm
- [ ] Clock-in should create a time session with a start_time
- [ ] Clock-out should update a time session's end_time
- [ ] Clock-in on another task should update a session's end_time + interrupted_by_task_id
- [ ] A fsm subscription should render a permanent toas with clock out butto
