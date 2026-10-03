# Task Tracker CLI

A simple command line app to track your tasks and manage your to-do list, built in Java with no external libraries. Tasks are stored in a JSON file (`tasks.json`) in the current directory.

This project is based on the [Task Tracker](https://roadmap.sh/projects/task-tracker) project from roadmap.sh.

## Features

- Add, update and delete tasks
- Mark a task as in progress or done
- List all tasks
- List tasks by status: todo, in-progress, done
- Clear error messages for invalid input and a corrupted JSON file

## Requirements

- Java 11 or newer (check with `java -version`)

## How to run

Compile once:

```
javac TaskCli.java
```

Then run commands:

```
java TaskCli <command> [arguments]
```

You can also run without compiling (Java 11+):

```
java TaskCli.java <command> [arguments]
```

## Commands

```
# Add a new task
java TaskCli add "Buy groceries"
# Output: Task added successfully (ID: 1)

# Update a task
java TaskCli update 1 "Buy groceries and cook dinner"

# Delete a task
java TaskCli delete 1

# Mark a task as in progress or done
java TaskCli mark-in-progress 1
java TaskCli mark-done 1

# List tasks
java TaskCli list
java TaskCli list todo
java TaskCli list in-progress
java TaskCli list done
```

## Task format

Each task in `tasks.json` has these properties:

| Property    | Description                                      |
|-------------|--------------------------------------------------|
| id          | Unique number for the task                       |
| description | Short description of the task                    |
| status      | `todo`, `in-progress` or `done`                  |
| createdAt   | Date and time the task was created               |
| updatedAt   | Date and time the task was last changed          |

Example:

```json
[
  {
    "id": 1,
    "description": "Buy groceries and cook dinner",
    "status": "done",
    "createdAt": "2026-10-02T22:06:49",
    "updatedAt": "2026-10-03T22:02:26"
  }
]
```

## How it works

- `tasks.json` is created automatically if it does not exist.
- New task IDs are the largest existing ID plus one.
- JSON is written and read by hand (a small parser inside `TaskCli.java`), because the project does not allow external libraries.

## Error handling

The app prints a clear message instead of crashing for:

- an unknown command or missing arguments
- a task ID that is not a number
- a task ID that does not exist
- an empty description
- an invalid or corrupted `tasks.json`
