SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE SCHEMA IF NOT EXISTS `NCBI` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;
USE `NCBI` ;

-- -----------------------------------------------------
-- Table `NCBI`.`names`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `NCBI`.`names` (
  `taxid` INT UNSIGNED NOT NULL ,
  `name` VARCHAR(200) NOT NULL COMMENT 'Despite the NCBI database contains a lot of weird redundant names, the one that we are using is exclusively \\\"scientific name\\\"' COMMENT 'Need this to make the search faster in the database by taxid or by name' ,
  INDEX `ind_param` (`taxid` ASC, `name` ASC) ,
  PRIMARY KEY (`taxid`) ,
  UNIQUE INDEX `taxid_UNIQUE` (`taxid` ASC) ,
  INDEX `ind_name` (`name` ASC) )
ENGINE = InnoDB
COMMENT = 'This table contains pairs of TaxIDs with the normal taxonomic names of the organisms and their class (sinonym).';


-- -----------------------------------------------------
-- Table `NCBI`.`GI_TAXID`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `NCBI`.`GI_TAXID` (
  `gi` BIGINT UNSIGNED NOT NULL COMMENT 'Bigint is good because the last time i talked to a guy from NCBI he told me that they were running out of int pretty soon.' ,
  `taxid` INT UNSIGNED NOT NULL ,
  PRIMARY KEY (`gi`) ,
  INDEX `fk_GI_TAXID_names_idx` (`taxid` ASC) ,
  UNIQUE INDEX `gi_UNIQUE` (`gi` ASC) ,
  INDEX `ind_taxid` (`taxid` ASC) ,
  CONSTRAINT `fk_GI_TAXID_names`
    FOREIGN KEY (`taxid` )
    REFERENCES `NCBI`.`names` (`taxid` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'This table represents a GI-TaxID pair, and thereby allows to trace back to the taxonomy by a given GI number.';


-- -----------------------------------------------------
-- Table `NCBI`.`ranks`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `NCBI`.`ranks` (
  `id_ranks` INT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `rank` VARCHAR(200) NOT NULL ,
  PRIMARY KEY (`id_ranks`) ,
  UNIQUE INDEX `id_ranks_UNIQUE` (`id_ranks` ASC) ,
  INDEX `ind_rank` (`rank` ASC) )
ENGINE = InnoDB
COMMENT = 'This table is a validation for the ranks in \"nodes\" table, helps decrease redundancy that is all over the place in NCBI database tables.';


-- -----------------------------------------------------
-- Table `NCBI`.`nodes`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `NCBI`.`nodes` (
  `id_nodes` INT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `taxid` INT UNSIGNED NOT NULL ,
  `parent_taxid` INT UNSIGNED NOT NULL COMMENT 'TaxID is unique, but the parent TaxID is not\\n' ,
  `id_ranks` INT UNSIGNED NOT NULL ,
  PRIMARY KEY (`id_nodes`) ,
  UNIQUE INDEX `id_nodes_UNIQUE` (`id_nodes` ASC) ,
  UNIQUE INDEX `taxid_UNIQUE` (`taxid` ASC) COMMENT 'Makes the search faster by a given parameter' ,
  INDEX `ind_param` (`taxid` ASC, `parent_taxid` ASC, `id_ranks` ASC) ,
  INDEX `fk_nodes_ranks1_idx` (`id_ranks` ASC) ,
  INDEX `fk_nodes_nodes1_idx` (`parent_taxid` ASC) ,
  INDEX `ind_parent_taxid` (`parent_taxid` ASC) ,
  CONSTRAINT `fk_nodes_names1`
    FOREIGN KEY (`taxid` )
    REFERENCES `NCBI`.`names` (`taxid` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_nodes_ranks1`
    FOREIGN KEY (`id_ranks` )
    REFERENCES `NCBI`.`ranks` (`id_ranks` )
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_nodes_nodes1`
    FOREIGN KEY (`parent_taxid` )
    REFERENCES `NCBI`.`nodes` (`taxid` )
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB
COMMENT = 'A table of taxonomic nodes that contains daughter-parent taxonomic relationship. Makes it usefull to reconstruct the full taxonomic tree breanch for a given taxid.';


-- -----------------------------------------------------
-- Placeholder table for view `NCBI`.`taxon_by_gi`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `NCBI`.`taxon_by_gi` (`gi` INT, `taxid` INT, `name` INT, `rank` INT, `id_ranks` INT);

-- -----------------------------------------------------
-- Placeholder table for view `NCBI`.`f_level_children_by_parent`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `NCBI`.`f_level_children_by_parent` (`parent_taxid` INT, `taxid` INT, `name` INT, `rank` INT, `id_ranks` INT);

-- -----------------------------------------------------
-- Placeholder table for view `NCBI`.`rank_by_taxid`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `NCBI`.`rank_by_taxid` (`taxid` INT, `parent_taxid` INT, `rank` INT, `id_ranks` INT);

-- -----------------------------------------------------
-- View `NCBI`.`taxon_by_gi`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `NCBI`.`taxon_by_gi`;
USE `NCBI`;
CREATE  OR REPLACE VIEW `NCBI`.`taxon_by_gi` AS
SELECT
	GI_TAXID.gi,
	GI_TAXID.taxid,
	names.name,
	ranks.rank,
    ranks.id_ranks
FROM GI_TAXID
	JOIN names on GI_TAXID.taxid = names.taxid
	JOIN nodes on names.taxid=nodes.taxid
	JOIN ranks on nodes.id_ranks=ranks.id_ranks;

-- -----------------------------------------------------
-- View `NCBI`.`f_level_children_by_parent`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `NCBI`.`f_level_children_by_parent`;
USE `NCBI`;
CREATE  OR REPLACE VIEW `NCBI`.`f_level_children_by_parent` AS
	SELECT nodes.parent_taxid, nodes.taxid, names.name, ranks.rank, ranks.id_ranks
	FROM nodes
	JOIN ranks ON nodes.id_ranks=ranks.id_ranks
	JOIN names ON nodes.taxid=names.taxid;

-- -----------------------------------------------------
-- View `NCBI`.`rank_by_taxid`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `NCBI`.`rank_by_taxid`;
USE `NCBI`;
CREATE  OR REPLACE VIEW `NCBI`.`rank_by_taxid` AS SELECT nodes.taxid, nodes.parent_taxid, ranks.rank, ranks.id_ranks
	FROM nodes
	JOIN ranks ON nodes.id_ranks=ranks.id_ranks;

CREATE USER 'tuit'@'localhost' IDENTIFIED BY 'tuit';
GRANT ALL PRIVILEGES ON `NCBI`.* to 'tuit'@'localhost';
GRANT FILE ON *.* TO 'tuit'@'localhost' IDENTIFIED BY 'tuit';

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

