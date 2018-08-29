var randomNumbers = [42, 12, 88, 62, 63, 56, 1, 77, 88, 97, 97, 20, 45, 91, 62, 2, 15, 31, 59, 5]

func quickSort(array: [Int]) -> [Int] {
    var less = [Int]()
    var equal = [Int]()
    var greater = [Int]()
    
    if array.count > 1 {
        let pivot = array[0]
        
        for x in array {
            if x < pivot {
                less.append(x)
            } else if x == pivot {
                equal.append(x)
            } else {
                greater.append(x)
            }
        }
        
        return (quickSort(array: less) + equal + quickSort(array: greater))
    } else {
        return array
    }
}

print(quickSort(array: randomNumbers))