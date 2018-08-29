#include "helper.cpp"
class A
{
public:
    int a = 5;
    double b = 6;
    int f(int d)
    {
        return 5;
    }
};
A c = A();
int main() {
    print(c.f(2));
    return 0;
}
