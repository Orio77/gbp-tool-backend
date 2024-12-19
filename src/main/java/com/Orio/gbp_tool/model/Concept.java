package com.Orio.gbp_tool.model;

import java.util.List;
import lombok.Value;

@Value
public class Concept {
    String name;
    List<String> associatedTexts;
}
