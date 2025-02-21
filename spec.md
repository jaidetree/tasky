# Time Tracking Tool Technical Specification

## Overview
A personal productivity tool for tracking time spent on tasks, comparing actual time against estimates, and managing task completion states.

## System Architecture
- Web application running locally
- PostgreSQL database for data persistence
- Accessible via Tailscale for sharing/access
- No authentication required for initial version

## Data Models

### Task
- id: UUID (primary key)
- title: string (required)
- notes: text (optional)
- estimated_time: integer (minutes, required)
- due_date: datetime (optional)
- created_at: datetime
- completed_at: datetime (nullable)
- deleted_at: datetime (nullable)
- parent_task_id: UUID (foreign key to Task, nullable)

### TimeSession
- id: UUID (primary key)
- task_id: UUID (foreign key to Task)
- start_time: datetime
- end_time: datetime
- original_end_time: datetime (nullable, for storing pre-edit value)
- notes: text (optional)
- interrupted_by_task_id: UUID (nullable, references Task)

## Core Functionality

### Task Management
- Tasks can be created with title, notes, and estimated completion time
- Tasks can have unlimited nested subtasks
- Parent tasks have independent time estimates from their subtasks
- Tasks are displayed in a table view, sorted by created_at (newest first) by default
- Alternative sorting available by due date and completion status
- Tasks can be marked as complete one at a time
- Completed tasks remain visible but are visually grayed out
- Tasks can be marked as deleted (soft delete) and hidden from view
- Due dates are stored as datetime values

### Time Tracking
- Users can start a time session for any task
- Starting a new session while another is running will:
  1. Show a warning to the user
  2. If user continues, end the previous session
  3. Add a note to the previous session indicating it was interrupted for the new task
- Active timer displays in hours:minutes:seconds format with real-time updates
- Sessions can be ended manually by the user
- Session end times can be edited after the fact
  - Original end time is preserved in original_end_time field
  - UI indicates when a session has been manually edited
- Sessions can be deleted but cannot be manually created
- No pause functionality - breaks are tracked as separate sessions

## User Interface

### Main Layout
- Sidebar containing:
  - Currently active task information
  - Elapsed time
  - Estimated time
  - List of sessions and notes for active task
- Main table view of all tasks

### Task Table
- Columns:
  - Title
  - Estimated time
  - Due date (if set)
  - Completion status
  - Actions (start/stop timer, complete task, etc.)
- Visual Indicators:
  - Completed tasks are grayed out
  - Tasks with upcoming due dates are highlighted
  - Active task is visually distinguished

### Time Comparison
- For completed tasks, shows percentage over/under estimated time
- Color coding:
  - Green: actual time > estimated time
  - Red: actual time ≤ estimated time

## Future Considerations
The following features are explicitly out of scope for the initial version:
- User authentication
- Task search and filtering
- Data export functionality
- Recurring tasks
- Bulk task operations
- Analytics and reporting
- Multi-device synchronization

## Technical Requirements
- Backend: Any web framework compatible with PostgreSQL
- Frontend: Any framework capable of real-time timer updates
- Database: PostgreSQL
- Network: Tailscale for sharing/access
