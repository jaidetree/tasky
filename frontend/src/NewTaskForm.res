open Preact
open State

@jsx.component
let make = (~state: NewTaskFSM.state) => {
  let _state = state
  let onSubmit = e => {
    e->JsxEvent.Form.preventDefault
  }
  <form onSubmit className="gap-2">
    <header>
      <h2 className="text-xl"> {"New Task"->string} </h2>
    </header>
    <section>
      <label htmlFor="id_title" className="block"> {"Title"->string} </label>
      <input type_="text" name="title" />
    </section>
  </form>
}
