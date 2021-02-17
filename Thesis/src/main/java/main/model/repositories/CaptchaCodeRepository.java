package main.model.repositories;

import main.model.CaptchaCodes;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaptchaCodeRepository extends CrudRepository<CaptchaCodes, Integer> {

}
