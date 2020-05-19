package com.bl.learningmanagementsystem.service;

import com.bl.learningmanagementsystem.dto.HiredCandidate;
import com.bl.learningmanagementsystem.model.HiredCandidateModel;

import java.io.IOException;
import java.util.List;

public interface HiredCandidateService {
    List getHiredCandidate(String filePath) throws IOException;

    void save(HiredCandidate hiredCandidate);

    List getHiredCandidates();

    HiredCandidateModel findByFirst_name(String name);
}
