func printElement(arr: inout [[Int]]) {
    var begin: Int = 0
    for a in arr {
        for b in a {
            print(b)
        }
    }
}
var c = [[1,2,3,4],[5,6,7,8]]
printElement(c)
