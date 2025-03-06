// Generated by ReScript, PLEASE EDIT WITH CARE

import * as Task from "./Task.res.mjs";
import * as DateTime from "./DateTime.res.mjs";
import * as Belt_Array from "rescript/lib/es6/belt_Array.js";
import * as Signals from "@preact/signals";

var actionSignal = Signals.signal("Init");

var stateSignal = Signals.signal("Inactive");

function reduce(prevState, action) {
  var exit = 0;
  if (typeof action !== "object") {
    if (action === "Init") {
      return prevState;
    }
    exit = 2;
  } else {
    exit = 2;
  }
  if (exit === 2) {
    if (typeof prevState !== "object") {
      if (typeof action !== "object" || action.TAG !== "Fetch") {
        return prevState;
      } else {
        return {
                TAG: "Fetching",
                _0: Task.fetchTask(action._0)
              };
      }
    }
    switch (prevState.TAG) {
      case "Fetching" :
          if (typeof action !== "object" || action.TAG !== "Fetched") {
            return prevState;
          } else {
            return {
                    TAG: "Idle",
                    _0: action._0
                  };
          }
      case "Error" :
          break;
      case "Idle" :
          if (typeof action !== "object") {
            if (action === "ClockIn") {
              return {
                      TAG: "Running",
                      _0: prevState._0,
                      _1: {
                        id: "",
                        started_at: DateTime.now(),
                        ended_at: undefined,
                        interrupted_by_task_id: undefined,
                        notes: ""
                      }
                    };
            } else {
              return prevState;
            }
          }
          switch (action.TAG) {
            case "Fetch" :
                return prevState;
            case "Fetched" :
                break;
            case "UpdateTask" :
                return {
                        TAG: "Idle",
                        _0: action._0
                      };
            
          }
          break;
      case "Running" :
          var session = prevState._1;
          var task = prevState._0;
          if (typeof action !== "object") {
            if (action === "ClockIn") {
              return prevState;
            } else {
              return {
                      TAG: "Idle",
                      _0: {
                        id: task.id,
                        name: task.name,
                        notes: task.notes,
                        parent_task_id: task.parent_task_id,
                        estimated_time: task.estimated_time,
                        time_sessions: Belt_Array.concatMany([
                              task.time_sessions,
                              [{
                                  id: session.id,
                                  started_at: session.started_at,
                                  ended_at: DateTime.now(),
                                  interrupted_by_task_id: session.interrupted_by_task_id,
                                  notes: session.notes
                                }]
                            ]),
                        tasks: task.tasks
                      }
                    };
            }
          }
          switch (action.TAG) {
            case "Fetch" :
                return prevState;
            case "Fetched" :
                break;
            case "UpdateTask" :
                return {
                        TAG: "Running",
                        _0: action._0,
                        _1: session
                      };
            
          }
          break;
      
    }
  }
  return prevState;
}

function dispatch(action) {
  actionSignal.value = action;
}

var TaskFSM = {
  actionSignal: actionSignal,
  stateSignal: stateSignal,
  reduce: reduce,
  dispatch: dispatch
};

var actionSignal$1 = Signals.signal("Init");

var stateSignal$1 = Signals.signal("Inactive");

function reduce$1(prevState, action) {
  if (typeof action !== "object" && action === "Init") {
    return prevState;
  }
  if (typeof prevState !== "object") {
    if (typeof action !== "object" && action === "NewTask") {
      return {
              TAG: "Active",
              _0: {
                id: "",
                name: "",
                notes: "",
                parent_task_id: undefined,
                estimated_time: 0,
                time_sessions: [],
                tasks: []
              }
            };
    } else {
      return prevState;
    }
  }
  if (prevState.TAG !== "Active") {
    if (typeof action !== "object" || action.TAG !== "Saved") {
      return prevState;
    } else {
      return "Inactive";
    }
  }
  if (typeof action === "object") {
    if (action.TAG === "Update") {
      return {
              TAG: "Active",
              _0: action._0
            };
    } else {
      return prevState;
    }
  }
  if (action === "NewTask") {
    return prevState;
  }
  var promise = Task.createTask(prevState._0);
  return {
          TAG: "Saving",
          _0: promise.then(function (task) {
                actionSignal$1.value = {
                  TAG: "Saved",
                  _0: task
                };
                return Promise.resolve(task);
              })
        };
}

var NewTaskFSM = {
  actionSignal: actionSignal$1,
  stateSignal: stateSignal$1,
  reduce: reduce$1
};

var actionSignal$2 = Signals.signal("Init");

var stateSignal$2 = Signals.signal("Idle");

function reduce$2(prevState, action) {
  if (typeof action !== "object") {
    if (action === "Init") {
      return prevState;
    } else {
      return {
              TAG: "CreatingTask",
              _0: reduce$1("Inactive", "NewTask")
            };
    }
  }
  if (typeof prevState !== "object") {
    if (typeof action === "object") {
      switch (action.TAG) {
        case "OpenTask" :
            return {
                    TAG: "Task",
                    _0: reduce("Inactive", {
                          TAG: "Fetch",
                          _0: action._0
                        })
                  };
        case "NewTaskFSM" :
        case "TaskFSM" :
            return prevState;
        
      }
    }
    
  } else if (prevState.TAG === "Task") {
    if (typeof action === "object") {
      switch (action.TAG) {
        case "OpenTask" :
            return {
                    TAG: "Task",
                    _0: reduce("Inactive", {
                          TAG: "Fetch",
                          _0: action._0
                        })
                  };
        case "NewTaskFSM" :
            return prevState;
        case "TaskFSM" :
            return {
                    TAG: "Task",
                    _0: reduce(prevState._0, action._0)
                  };
        
      }
    }
    
  } else if (typeof action === "object") {
    switch (action.TAG) {
      case "OpenTask" :
          return {
                  TAG: "Task",
                  _0: reduce("Inactive", {
                        TAG: "Fetch",
                        _0: action._0
                      })
                };
      case "NewTaskFSM" :
          return {
                  TAG: "CreatingTask",
                  _0: reduce$1(prevState._0, action._0)
                };
      case "TaskFSM" :
          return prevState;
      
    }
  }
  
}

var transitionSignal = Signals.signal({
      prev: "Idle",
      next: "Idle",
      action: "Init"
    });

function dispatch$1(action) {
  var prevState = stateSignal$2.peek();
  var nextState = reduce$2(prevState, action);
  stateSignal$2.value = nextState;
  transitionSignal.value = {
    prev: prevState,
    next: nextState,
    action: action
  };
}

var AppFSM = {
  actionSignal: actionSignal$2,
  stateSignal: stateSignal$2,
  reduce: reduce$2,
  transitionSignal: transitionSignal,
  dispatch: dispatch$1
};

Signals.effect(function () {
      var transition = transitionSignal.value;
      console.log("transition", transition);
    });

export {
  TaskFSM ,
  NewTaskFSM ,
  AppFSM ,
}
/* actionSignal Not a pure module */
