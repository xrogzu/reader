alter table T_FEED add column FED_RSSBASEURI_C varchar(2000);
alter table T_ARTICLE add column ART_BASEURI_C varchar(2000);
create cached table T_JOB (JOB_ID_C varchar(36) not null, JOB_IDUSER_C varchar(36), JOB_NAME_C varchar(50) not null, JOB_CREATEDATE_D datetime not null, JOB_STARTDATE_D datetime, JOB_ENDDATE_D datetime, JOB_DELETEDATE_D datetime, primary key (JOB_ID_C));
create cached table T_JOB_EVENT ( JOE_ID_C varchar(36) not null, JOE_IDJOB_C varchar(36), JOE_NAME_C varchar(50) not null, JOE_VALUE_C varchar(250), JOE_CREATEDATE_D datetime not null, JOE_DELETEDATE_D datetime, primary key (JOE_ID_C));
alter table T_JOB add constraint FK_JOB_IDUSER_C foreign key (JOB_IDUSER_C) references T_USER (USE_ID_C) on delete restrict on update restrict;
alter table T_JOB_EVENT add constraint FK_JOE_IDJOB_C foreign key (JOE_IDJOB_C) references T_JOB (JOB_ID_C) on delete restrict on update restrict;
update T_CONFIG set CFG_VALUE_C='1' where CFG_ID_C='DB_VERSION';
create index IDX_ART_PUBLICATIONDATE_D on T_ARTICLE ( ART_PUBLICATIONDATE_D );
