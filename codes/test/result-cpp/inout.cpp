#include "helper.cpp"
int f(int & a)
{
    a = 10;
    return 8;
}
int b = 5;
int main() {
    print(f(b) + b);
    return 0;
}
