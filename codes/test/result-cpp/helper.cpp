#include <vector>
#include <map>
#include <iostream>
#include <string>
using namespace std;
template <typename T>
vector<T> operator + (vector<T> a, vector<T> b) {
    a.insert(a.end(), b.begin(), b.end());
    return a;
}

template <typename T>
void printt(T a) {
    cout << a;
}

template <typename T>
void printt(vector<T> a) {
    cout << "[";
    
    int l = (int)a.size();
    for (int i = 0; i < l; ++i) {
        printt(a[i]);
        if (i != l - 1) {
            cout << ", ";
        }
    }
    
    cout << "]";
}

template <typename T1, typename T2>
void printt(map<T1, T2> m) {
    cout << "[";

    int l = (int)m.size();
    for (typename map<T1, T2>::iterator it = m.begin(); it != m.end(); ++it, --l) {
        printt(it->first);
        cout << ": ";
        printt(it->second);
        if (l != 1) {
            cout << ", ";
        }
    }

    cout << "]";
}

template <typename T>
void print(vector<T> a) {
    cout << "[";
    
    int l = (int)a.size();
    for (int i = 0; i < l; ++i) {
        printt(a[i]);
        if (i != l - 1) {
            cout << ", ";
        }
    }
    
    cout << "]" << endl;
}

template <typename T>
void print(T a) {
    cout << a << endl;
}

template <typename T1, typename T2>
void print(map<T1, T2> m) {
    cout << "[";

    int l = (int)m.size();
    for (typename map<T1, T2>::iterator it = m.begin(); it != m.end(); ++it, --l) {
        printt(it->first);
        cout << ": ";
        printt(it->second);
        if (l != 1) {
            cout << ", ";
        }
    }

    cout << "]" << endl;
}