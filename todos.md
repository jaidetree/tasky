```markdown
# To-Do Checklist for Time Tracking Tool Project

This checklist covers all major tasks required to build the Time Tracking Tool. Each section is broken down into actionable items. Check items off as you complete them.

---

## 1. Project Setup & Environment

- [x] **Repository Initialization**

  - [x] Initialize Git repository.
  - [x] Create basic folder structure:
    - `/backend` for API code.
    - `/frontend` for UI code.
    - `/tests` for backend and frontend tests.
  - [x] Create a `README.md` with project overview and setup instructions.

- [ ] **Basic Application Skeleton**
  - [ ] Create a simple “Hello World” endpoint in the backend that returns a JSON message.
  - [ ] Develop a static HTML page in `/frontend` displaying “Time Tracking Tool” and a timer placeholder.
  - [ ] Wire the backend to serve the static frontend files.
  - [ ] Write and run basic unit tests for the “Hello World” endpoint.

---

## 2. Database Setup & Data Models

- [x] **PostgreSQL Integration**

  - [x] Configure backend to connect to PostgreSQL.
  - [x] Verify database connectivity.

- [x] **Task Model**

  - [x] Define the following fields:
    - `id` (UUID, primary key)
    - `title` (string, required)
    - `notes` (text, optional)
    - `estimated_time` (integer, required, minutes)
    - `due_date` (datetime, optional)
    - `created_at` (datetime)
    - `completed_at` (datetime, nullable)
    - `deleted_at` (datetime, nullable)
    - `parent_task_id` (UUID, nullable, foreign key referencing Task)

- [x] **TimeSession Model**

  - [x] Define the following fields:
    - `id` (UUID, primary key)
    - `task_id` (UUID, foreign key referencing Task)
    - `start_time` (datetime)
    - `end_time` (datetime)
    - `original_end_time` (datetime, nullable)
    - `notes` (text, optional)
    - `interrupted_by_task_id` (UUID, nullable, references Task)

- [x] **Database Migrations**

  - [x] Create migration scripts for Task and TimeSession tables.
  - [ ] Run migrations and verify tables are created.

- [ ] **Model Testing**
  - [ ] Write unit tests to instantiate models.
  - [ ] Verify required fields and constraints.

---

## 3. Backend API Development

### Task API Endpoints

- [ ] **Create Task**

  - [ ] Develop an endpoint to create a new Task (fields: title, notes, estimated_time, due_date, optional parent_task_id).
  - [ ] Validate required fields.

- [ ] **Read Tasks**

  - [ ] Develop an endpoint to list all tasks.
  - [ ] Implement default sorting by `created_at` (newest first).
  - [ ] Add options to sort by due date and completion status.

- [ ] **Update Task**

  - [ ] Develop an endpoint to update task details.

- [ ] **Complete Task**

  - [ ] Develop an endpoint to mark a task as complete (set `completed_at`).
  - [ ] Ensure completed tasks are visible but styled differently (e.g., grayed out).

- [ ] **Soft Delete Task**

  - [ ] Develop an endpoint to mark a task as deleted (set `deleted_at`).

- [ ] **API Testing**
  - [ ] Write tests for each endpoint:
    - Verify task creation, reading, updating.
    - Verify task completion and soft deletion.
  - [ ] Wire endpoints into the backend router.

---

## 4. TimeSession API Development

- [ ] **Start TimeSession Endpoint**

  - [ ] Create an endpoint to start a new session for a given task.
  - [ ] Check if a session is already active.
    - [ ] If active, return a warning message.
    - [ ] Allow user confirmation (simulate via API flag) to end the active session.
    - [ ] End previous session by setting its `end_time` and add a note indicating interruption.

- [ ] **Stop TimeSession Endpoint**

  - [ ] Create an endpoint to manually end the current session.

- [ ] **Edit TimeSession End Time**

  - [ ] Develop an endpoint to update the session’s `end_time`.
  - [ ] Preserve original `end_time` in `original_end_time` when edited.
  - [ ] Provide UI indication for manually edited sessions.

- [ ] **Delete TimeSession Endpoint**

  - [ ] Develop an endpoint to delete a time session.

- [ ] **TimeSession Testing**

  - [ ] Write tests for starting, stopping, editing, and deleting sessions.
  - [ ] Verify logic for interrupting an active session.

- [ ] **API Routing**
  - [ ] Wire all TimeSession endpoints into the application’s routing configuration.

---

## 5. Frontend UI Development

### Task Table View

- [ ] **Build Task Table Component**
  - [ ] Fetch tasks from the backend.
  - [ ] Display columns: Title, Estimated Time, Due Date, Completion Status, Actions.
  - [ ] Style:
    - [ ] Gray out completed tasks.
    - [ ] Highlight tasks with upcoming due dates.

### Active Timer Sidebar

- [ ] **Build Active Timer Component**
  - [ ] Display current active task details.
  - [ ] Implement a real-time timer (hh:mm:ss format).
  - [ ] Show the estimated time and list of session notes.

### Integration

- [ ] **Task Selection Wiring**

  - [ ] Ensure selecting a task from the table sets it as the active task in the sidebar.

- [ ] **Frontend Testing**
  - [ ] Write or outline a testing strategy for UI components (unit and integration tests).
  - [ ] Verify that task list fetch, timer updates, and UI state changes work as expected.
  - [ ] Confirm that the frontend correctly interacts with backend API endpoints.

---

## 6. Advanced UI Components & Integration

- [ ] **Session Management UI**

  - [ ] Add UI controls for starting and stopping time sessions.
  - [ ] Implement a modal/prompt to warn the user about an active session when starting a new one.

- [ ] **Session Editing UI**

  - [ ] Provide an interface to edit a session’s end time.
  - [ ] Indicate if a session’s end time has been manually edited.

- [ ] **End-to-End Integration**

  - [ ] Wire UI controls to trigger corresponding API endpoints.
  - [ ] Ensure real-time UI updates reflecting backend changes.

- [ ] **Advanced Testing**
  - [ ] Write integration tests verifying:
    - Session interruption logic.
    - Correct updating of `original_end_time` on edits.
    - Consistency between UI state and backend data.

---

## 7. Final Integration, Testing, & Deployment Preparations

- [ ] **End-to-End Testing**

  - [ ] Write tests simulating complete user flows:
    - Creating a task.
    - Starting a time session.
    - Interrupting an active session.
    - Editing a session’s end time.
    - Marking tasks complete or deleting them.

- [ ] **Error Handling & Edge Cases**

  - [ ] Ensure APIs handle invalid inputs and database errors gracefully.
  - [ ] Provide user-friendly error messages in the UI.

- [ ] **Final Wiring & Code Cleanup**

  - [ ] Confirm that all backend endpoints, UI components, and database models are fully integrated.
  - [ ] Remove any orphaned or unused code.

- [ ] **Documentation & Deployment**
  - [ ] Update `README.md` with:
    - Setup instructions.
    - How to run the application locally.
    - Steps to run all tests.
    - Deployment notes (e.g., configuring Tailscale for sharing the app).
  - [ ] Prepare deployment configuration if necessary.

---

## Bonus: Future Considerations (Out of Scope for Initial Version)

- [ ] Plan for future enhancements:
  - [ ] User authentication.
  - [ ] Task search and filtering.
  - [ ] Data export functionality.
  - [ ] Recurring tasks.
  - [ ] Bulk operations.
  - [ ] Analytics and reporting.
  - [ ] Multi-device synchronization.

---

Keep this checklist updated as you progress, and mark each item complete to track your project’s progress. Happy coding!
```
