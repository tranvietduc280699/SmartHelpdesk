package org.example.besmarthelpdesk.service;

import org.example.besmarthelpdesk.entity.Member;

public interface AutoAssignmentService {
    Member selectBestDeveloper();
}
