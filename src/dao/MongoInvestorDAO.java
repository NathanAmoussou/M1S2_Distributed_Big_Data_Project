package src.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import src.model.Investor;
import src.util.MongoDBConnection;

import java.util.List;

public class MongoInvestorDAO implements InvestorDAO {
    private MongoCollection<Document> investorCollection;

    public MongoInvestorDAO(){
        MongoDatabase database = MongoDBConnection.getInstance().getDatabase();
        investorCollection = database.getCollection("investors");
    }

    @Override
    public void createInvestor(Investor investor) {

    }

    @Override
    public Investor getInvestorById(String investorId) {
        return null;
    }

    @Override
    public List<Investor> getAllInvestors() {
        return List.of();
    }

    @Override
    public void updateInvestor(Investor investor) {

    }

    @Override
    public void deleteInvestor(String investorId) {

    }
}
