package com.dku.council.domain.user.repository;

import com.dku.council.domain.user.model.entity.Major;
import com.sun.xml.bind.v2.util.QNameMap;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {
    @Query("select m from Major m where m.name=:name and m.department=:department and m.isActive=true")
    Optional<Major> findByName(String name, String department);

    @Query("select m from Major m where m.id=:id and m.isActive=true")
    Optional<Major> findById(Long id);

    @Query("select m from Major m where m.isActive=true")
    List<Major> findAll();
}
