@val @scope("Date") external jsNow: unit => float = "now"

let now = () => {
  jsNow() /. 1000.0
}
