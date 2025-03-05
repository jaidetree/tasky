%%raw("import 'vite/modulepreload-polyfill'")
%%raw("import './index.css'")
open Preact

switch Doc.querySelector("#sidebar-root") {
| Some(domElement) => render(<Sidebar />, domElement)
| None => ()
}

@val external window: 'window = "window"

window["openSidebar"] = (button: Dom.element) => {
  Js.Console.log(button)
}

window["newTask"] = (button: Dom.element) => {
  State.AppFSM.dispatch(NewTask)
}
