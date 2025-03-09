%%raw("import 'vite/modulepreload-polyfill'")
%%raw("import './index.css'")
open Preact

// switch Doc.querySelector("#sidebar-root") {
// | Some(domElement) => render(<Sidebar />, domElement)
// | None => ()
// }
//

switch Doc.querySelector("#root") {
| Some(domElement) => render(<App />, domElement)
| None => ()
}

@val external window: 'window = "window"

// window["openSidebar"] = (button: Dom.element) => {
//   Js.Console.log(button)
// }
//
// window["newTask"] = (_button: Dom.element) => {
//   State.AppFSM.dispatch(NewTask(Create))
// }
