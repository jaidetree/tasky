Prompt 1: Project Setup & Basic Structure

You are to set up a new repository for a Time Tracking Tool. Create the following basic folder structure:
- /backend: for API code (use Elixir + Phoenix with PostgreSQL support)
- /frontend: for UI code (a simple web page with a real-time timer placeholder)
- /tests: for both backend and frontend tests

Within the repository, include:
1. A README.md outlining the project and setup instructions.
2. A basic “Hello World” endpoint in the backend that returns a simple JSON message.
3. A simple static HTML page in the frontend that displays “Time Tracking Tool” and a placeholder for the timer.

Ensure that the project is version controlled (assume Git). At the end, wire the backend and frontend together by serving the static files from the backend.

Please also include basic unit tests for the “Hello World” endpoint in the tests folder. This prompt should yield a minimal working application that can be run locally.

Prompt 2: Database Setup & Data Models

Now that we have the basic structure, let’s add the PostgreSQL database integration and create the data models. Implement the following:

1. Configure the backend to connect to a PostgreSQL database.
2. Define the Task model with the following fields:
   - id: UUID (primary key)
   - title: string (required)
   - notes: text (optional)
   - estimated_time: integer (minutes, required)
   - due_date: datetime (optional)
   - created_at: datetime
   - completed_at: datetime (nullable)
   - deleted_at: datetime (nullable)
   - parent_task_id: UUID (nullable, foreign key referencing Task)
3. Define the TimeSession model with the following fields:
   - id: UUID (primary key)
   - task_id: UUID (foreign key referencing Task)
   - start_time: datetime
   - end_time: datetime
   - original_end_time: datetime (nullable)
   - notes: text (optional)
   - interrupted_by_task_id: UUID (nullable, references Task)
4. Create migration scripts to set up these tables.
5. Write unit tests that instantiate these models and check that validations (e.g., required fields) work as expected.

At the end of this prompt, ensure that the models are wired into the application and that the tests pass.

Prompt 3: Implementing Task API Endpoints

Build the API endpoints for Task management. Implement the following endpoints in the backend:

1. **Create Task:** An endpoint that accepts title, notes, estimated_time, due_date, and an optional parent_task_id. It should create a new Task.
2. **Read Tasks:** An endpoint to list all tasks, sorted by created_at (newest first), with alternative sorting options by due date and completion status.
3. **Update Task:** An endpoint to update task details.
4. **Complete Task:** An endpoint to mark a task as completed (set completed_at) while keeping it visible (but marked as complete).
5. **Soft Delete Task:** An endpoint to mark a task as deleted (set deleted_at) so it is hidden from view.

Write tests for each endpoint to validate:
- Correct creation and retrieval of tasks.
- Updating tasks works as expected.
- Tasks are correctly marked as completed and soft-deleted.

Ensure that after writing each endpoint, you wire it into the backend router and update any necessary configuration so that all endpoints are reachable.

Prompt 4: Implementing TimeSession API Endpoints

Now, add API endpoints for managing time sessions. Implement the following:

1. **Start TimeSession:** Create an endpoint to start a new time session for a given task. If a session is already running, the endpoint should:
   - Warn the user (return an appropriate message),
   - If the user confirms (simulate this via a flag in the API), end the current session by setting its end_time,
   - Append a note to the ended session indicating it was interrupted by the new task.
2. **Stop TimeSession:** An endpoint to manually end the active session by setting its end_time.
3. **Edit TimeSession End Time:** An endpoint to update a session’s end_time. The original_end_time should be stored if the session is manually edited.
4. **Delete TimeSession:** An endpoint to delete a time session (only deletable via the API, not manually created).

Write tests to ensure:
- A session can be started and stopped correctly.
- Starting a new session interrupts an active session with a proper note.
- Editing and deleting sessions behave as expected.

Finally, wire these endpoints into the application’s routing configuration.

Prompt 5: Frontend UI – Task Table and Active Timer Component

Develop the frontend user interface components for the Time Tracking Tool. Implement the following:

1. **Task Table View:** Build a component that fetches the list of tasks from the backend and displays them in a table. The table should include columns for title, estimated_time, due_date, and completion status. Apply visual styling to:
   - Gray out completed tasks.
   - Highlight tasks with upcoming due dates.
2. **Active Timer Sidebar:** Develop a sidebar component that shows:
   - The currently active task,
   - A real-time timer displaying elapsed time (in hh:mm:ss format),
   - The estimated time for the active task,
   - A list of time sessions with their notes.
3. **Integration:** Wire the components so that selecting a task from the table sets it as the active task in the sidebar.

Write tests or provide a testing strategy (if using a UI testing framework) to ensure that:
- The task list is correctly fetched and rendered.
- The active timer updates in real time.
- The UI correctly reflects changes (e.g., a task being marked complete or deleted).

Ensure that the frontend interacts with the API endpoints created earlier.

Prompt 6: Advanced UI Components and Integration

Enhance the frontend with additional UI elements and integration:

1. **Session Management UI:**
   - Add controls for starting and stopping a time session from the task table or active task sidebar.
   - Implement a UI prompt or modal that warns the user when starting a new session if one is already active. This prompt should simulate the confirmation flag used in the backend.
2. **Session Editing:**
   - Create an interface that allows users to edit a session’s end time, with an indicator if the time was manually edited.
3. **End-to-End Wiring:**
   - Integrate these components so that actions (start, stop, edit, delete sessions) update the UI in real time and reflect in the backend.
4. **Testing:**
   - Write integration tests to verify that starting a session interrupts any active session (with the correct note),
   - Ensure that session edits are stored correctly (checking the original_end_time),
   - Verify that the UI state is consistent with the backend state.

At the end of this prompt, all UI components should be fully wired to the backend, with no orphaned code. The system should now support full task and session management with real-time feedback.

Prompt 7: Final Integration, Testing, and Deployment Preparations

Now that all features have been built, perform the final integration and testing phase:

1. **Integration Testing:**
   - Write end-to-end tests that simulate user flows (e.g., creating a task, starting a session, being warned about an active session, interrupting it, and editing session end times).
2. **Error Handling and Edge Cases:**
   - Ensure that the API and frontend properly handle errors (e.g., invalid inputs, database connectivity issues) and display useful error messages.
3. **Final Wiring:**
   - Verify that all components (backend API, database, frontend UI) are fully integrated.
   - Ensure that there is no hanging or orphaned code – every new piece should be linked to a previous step.
4. **Deployment Considerations:**
   - Provide instructions for running the application locally.
   - Outline steps to set up Tailscale or a similar solution for sharing the app locally.

Write tests for the entire workflow and document how a developer would start the app and run all tests. This prompt should yield a production-ready version of the Time Tracking Tool that is well-documented and fully integrated.


