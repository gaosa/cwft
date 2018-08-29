class A {
    var a: Int = 5
    var b: Double = 6
    func f(d:Int)->Int {
        return a+b
    }
}
var c = A()
print(c.f(d:2))
