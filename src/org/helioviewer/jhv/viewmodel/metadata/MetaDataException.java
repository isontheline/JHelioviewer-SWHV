package org.helioviewer.jhv.viewmodel.metadata;

@SuppressWarnings("serial")
class MetaDataException extends RuntimeException {

    // public MetaDataException() { super(); }

    public MetaDataException(String s) { super(s + " not found in metadata"); }

}