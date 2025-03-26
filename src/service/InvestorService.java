package src.service;

import src.dao.InvestorDAO;
import src.dao.MongoInvestorDAO;
import src.model.Investor;

import java.util.List;

public class InvestorService {
    private InvestorDAO investorDAO;

    public InvestorService() {
        this.investorDAO = new MongoInvestorDAO();
    }

    public void createInvestor(Investor investor) {
        investorDAO.createInvestor(investor);
    }

    public Investor getInvestorById(String investorId) {
        return investorDAO.getInvestorById(investorId);
    }

    public void updateInvestor(Investor investor) {
        investorDAO.updateInvestor(investor);
    }

    public void deleteInvestor(String investorId) {
        investorDAO.deleteInvestor(investorId);
    }

    public List<Investor> getAllInvestors() {
        return investorDAO.getAllInvestors();
    }

}
