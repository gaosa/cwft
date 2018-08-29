#include "helper.cpp"
enum A
{
    a,b,c,
};
A cc = a;
int main() {
    switch (cc)
    {
        case a:
        print("haha");
        break;
        case b:
        print("hehe");
        break;
        default:
        print("hoho");
    }
    return 0;
}
