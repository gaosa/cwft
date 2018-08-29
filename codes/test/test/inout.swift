func f(a: inout Int) -> Int {
    a = 10
    return 8
}
var b = 5
print(f(a:&b)+b)
