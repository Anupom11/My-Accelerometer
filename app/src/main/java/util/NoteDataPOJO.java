package util;

import java.util.ArrayList;

public class NoteDataPOJO {

    ArrayList<String> uniqueIDList, noteDataList = new ArrayList<>();

    public ArrayList<String> getUniqueIDList() {
        return uniqueIDList;
    }

    public void setUniqueIDList(ArrayList<String> uniqueIDList) {
        this.uniqueIDList = uniqueIDList;
    }

    public ArrayList<String> getNoteDataList() {
        return noteDataList;
    }

    public void setNoteDataList(ArrayList<String> noteDataList) {
        this.noteDataList = noteDataList;
    }

}
