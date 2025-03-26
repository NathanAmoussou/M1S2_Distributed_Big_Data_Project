package src.dao;

import src.model.Investor;

import java.util.List;

public interface InvestorDAO {
    void createInvestor(Investor investor);

    Investor getInvestorById(String investorId);

    List<Investor> getAllInvestors();

    void updateInvestor(Investor investor);

    void deleteInvestor(String investorId);
}

