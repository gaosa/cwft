#include "helper.cpp"
vector<int> array1 = {1, 2, 3, 4, 5};
int main() {
    if (array1.size() > 1)
    {
    }
    else
    {
    }
    for (vector<int>::iterator it = array1.begin(); it != array1.end(); ++it)
    {
        int x = *it;
        print(x);
    }
    return 0;
}
