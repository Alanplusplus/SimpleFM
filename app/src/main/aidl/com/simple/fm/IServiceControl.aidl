// IServiceControl.aidl
package com.simple.fm;

// Declare any non-default types here with import statements

interface IServiceControl {
    void play();
    void stop();
    void resume();
    void pause();
    void setSource(String source);
    String getSource();


}
