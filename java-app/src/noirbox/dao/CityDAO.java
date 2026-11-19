package noirbox.dao;

import noirbox.db.JdbcHelper;
import noirbox.model.City;

import java.util.List;

/** Reads the city list used by the website's location selector. */
public class CityDAO {

    public List<City> findAll() {
        return JdbcHelper.query(
                "SELECT slug, city_name, state_name FROM city ORDER BY city_name",
                resultSet -> new City(
                        resultSet.getString("slug"),
                        resultSet.getString("city_name"),
                        resultSet.getString("state_name")));
    }
}
