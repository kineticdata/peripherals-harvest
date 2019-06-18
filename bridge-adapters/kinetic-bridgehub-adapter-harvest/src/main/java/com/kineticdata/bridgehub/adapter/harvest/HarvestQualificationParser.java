package com.kineticdata.bridgehub.adapter.harvest;

import com.kineticdata.bridgehub.adapter.QualificationParser;

public class HarvestQualificationParser extends QualificationParser {
    public String encodeParameter(String name, String value) {
        return value;
    }
}
