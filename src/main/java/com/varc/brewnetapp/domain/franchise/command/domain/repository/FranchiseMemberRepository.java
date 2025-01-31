package com.varc.brewnetapp.domain.franchise.command.domain.repository;

import com.varc.brewnetapp.domain.franchise.command.domain.aggregate.entity.FranchiseMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FranchiseMemberRepository extends JpaRepository<FranchiseMember, Integer> {

    Optional<FranchiseMember> findByMemberCode(Integer memberCode);

    List<FranchiseMember> findByActiveTrue();
}
