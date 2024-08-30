package com.Orio.gbp_tool.repository.impl.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Orio.gbp_tool.model.FileEntity;

@Repository
public interface FileRepo extends JpaRepository<FileEntity, Long> {

}
