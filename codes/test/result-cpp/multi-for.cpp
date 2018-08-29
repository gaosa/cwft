#include "helper.cpp"
void printElement (vector<vector<int>> & arr)
{
    int begin = 0;
    for (vector<vector<int>>::iterator it = arr.begin(); it != arr.end(); ++it)
    {
        vector<int> a = *it;
        for (vector<int>::iterator it = a.begin(); it != a.end(); ++it)
        {
            int b = *it;
            print(b);
        }
    }
}
vector<vector<int>> c = {{1, 2, 3, 4}, {5, 6, 7, 8}};
int main() {
    printElement(c);
    return 0;
}
