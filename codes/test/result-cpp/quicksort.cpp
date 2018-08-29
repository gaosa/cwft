#include "helper.cpp"
vector<int> randomNumbers = {42, 12, 88, 62, 63, 56, 1, 77, 88, 97, 97, 20, 45, 91, 62, 2, 15, 31, 59, 5};
vector<int> quickSort(vector<int> array)
{
    vector<int> less = vector<int>();
    vector<int> equal = vector<int>();
    vector<int> greater = vector<int>();
    if (array.size() > 1)
    {
        const int pivot = array[0];
        for (vector<int>::iterator it = array.begin(); it != array.end(); ++it)
        {
            int x = *it;
            if (x < pivot)
            {
                less.push_back(x);
            }
            else
            if (x == pivot)
            {
                equal.push_back(x);
            }
            else
            {
                greater.push_back(x);
            }
        }
        return (quickSort(less) + equal + quickSort(greater));
    }
    else
    {
        return array;
    }
}
int main() {
    print(quickSort(randomNumbers));
    return 0;
}
