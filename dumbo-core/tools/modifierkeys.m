#import <Carbon/Carbon.h>

// inspired by <http://lists.apple.com/archives/applescript-users/2009/Sep/msg00374.html>
//
// $ clang modifierkeys.m -framework Carbon -o modifierkeys
 
int main (int argc, const char * argv[]) {
    unsigned int modifiers = 0;
    for(int i=0;i<10;i++) {
      modifiers |= GetCurrentKeyModifiers();
    }

    modifiers >>= 8;

    // bit 0: command 
    // bit 1: shift
    // bit 2: caps lock
    // bit 3: option
    // bit 4: control
    // bit 5: right shift key?
    // bit 6: right option key?
    // bit 7: right control key?

    return modifiers;
}
