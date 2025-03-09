// Generated by ReScript, PLEASE EDIT WITH CARE

import * as State from "./State.res.mjs";
import * as Sidebar from "./Sidebar.res.mjs";
import * as TasksIndex from "./TasksIndex.res.mjs";
import * as Hooks from "preact/hooks";
import * as JsxRuntime from "preact/jsx-runtime";

function App(props) {
  var state = State.TasksFSM.stateSignal.value;
  Hooks.useEffect((function () {
          State.TasksFSM.dispatch("Fetch");
        }), []);
  var tmp;
  tmp = typeof state !== "object" ? "Tasky" : (
      state.TAG === "Loading" ? JsxRuntime.jsx("div", {
              children: "Loading..."
            }) : JsxRuntime.jsxs("main", {
              children: [
                JsxRuntime.jsx("div", {
                      children: JsxRuntime.jsx(TasksIndex.make, {
                            tasks: state._0.tasks
                          }),
                      className: "max-w-4xl flex-grow mx-auto px-4 py-20 overflow-auto"
                    }),
                JsxRuntime.jsx(Sidebar.make, {})
              ],
              className: "flex flex-row items-stretch min-h-screen"
            })
    );
  return JsxRuntime.jsx("div", {
              children: tmp
            });
}

var make = App;

export {
  make ,
}
/* State Not a pure module */
