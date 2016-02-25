
    alter table public.mmeteo_box 
        drop constraint FK_kkqqpjrbmmtaxqda44jb0pi

    alter table public.mmeteo_box_data 
        drop constraint FK_ebxqdker97v6n1bvgug44jd7n

    alter table public.sms 
        drop constraint FK_1edtqd3320935owxoaagtkxlg

    drop table if exists public.customer cascade

    drop table if exists public.meteo_data cascade

    drop table if exists public.meteo_data_archive cascade

    drop table if exists public.mmeteo_box cascade

    drop table if exists public.mmeteo_box_data cascade

    drop table if exists public.naio cascade

    drop table if exists public.sms cascade

    create table public.customer (
        id varchar(36) not null,
        creationTimestamp timestamp,
        modificationTimestamp timestamp,
        owner varchar(255),
        version int8,
        address varchar(255),
        city varchar(255),
        country varchar(255),
        customerStatus varchar(255),
        name varchar(255),
        phoneNumber varchar(255),
        postalCode varchar(255),
        social varchar(255),
        primary key (id)
    )

    create table public.meteo_data (
        id uuid not null,
        bytes_received int4,
        bytes_sent int4,
        client_phone1 varchar(255),
        client_phone2 varchar(255),
        client_phone3 varchar(255),
        data_date timestamp,
        data_humidity float8,
        data_temperature float8,
        humidity_max int4,
        humidity_min int4,
        imei varchar(255),
        mmeteo_date_version varchar(255),
        mmeteo_soft_version varchar(255),
        owner varchar(255),
        phone_number varchar(255),
        sms_received int4,
        sms_sent int4,
        temperature_max int4,
        temperature_min int4,
        ts timestamp,
        primary key (id)
    )

    create table public.meteo_data_archive (
        id varchar(36) not null,
        bytes_received int4,
        bytes_sent int4,
        client_phone1 varchar(255),
        client_phone2 varchar(255),
        client_phone3 varchar(255),
        data_date timestamp,
        data_humidity float8,
        data_temperature float8,
        humidity_max int4,
        humidity_min int4,
        imei varchar(255),
        mmeteo_date_version varchar(255),
        mmeteo_soft_version varchar(255),
        owner varchar(255),
        phone_number varchar(255),
        sms_received int4,
        sms_sent int4,
        temperature_max int4,
        temperature_min int4,
        ts timestamp,
        primary key (id)
    )

    create table public.mmeteo_box (
        id varchar(36) not null,
        creationTimestamp timestamp,
        modificationTimestamp timestamp,
        owner varchar(255),
        version int8,
        activityCheck boolean not null,
        activityTimeoutMinute int4 not null,
        alertTimeoutMinute int4 not null,
        clientPhone1 varchar(255),
        clientPhone2 varchar(255),
        clientPhone3 varchar(255),
        firstActivityDate timestamp,
        humidityMax int4 not null,
        humidityMin int4 not null,
        imei varchar(255),
        mmeteoDateVersion varchar(255),
        phoneNumber varchar(255),
        referenceNaio varchar(255) not null,
        softVersion varchar(255),
        temperatureMax int4 not null,
        temperatureMin int4 not null,
        customer varchar(36),
        primary key (id)
    )

    create table public.mmeteo_box_data (
        id varchar(36) not null,
        creationTimestamp timestamp,
        modificationTimestamp timestamp,
        owner varchar(255),
        version int8,
        bytesReceived int4 not null,
        bytesSent int4 not null,
        lastActivityCheck timestamp,
        lastHumidity float8,
        lastIncomingMessageDate timestamp,
        lastTemperature float8,
        smsReceived int4 not null,
        smsSent int4 not null,
        mmeteoBox varchar(36) not null,
        primary key (id)
    )

    create table public.naio (
        id varchar(36) not null,
        creationTimestamp timestamp,
        modificationTimestamp timestamp,
        owner varchar(255),
        version int8,
        alertPhoneNumber varchar(255) not null,
        email varchar(255) not null,
        name varchar(255) not null,
        sendEmailAlert boolean not null,
        sendServerError boolean not null,
        sendSmsAlert boolean not null,
        primary key (id)
    )

    create table public.sms (
        id varchar(36) not null,
        creationTimestamp timestamp,
        modificationTimestamp timestamp,
        owner varchar(255),
        version int8,
        archive boolean not null,
        error boolean not null,
        incoming boolean not null,
        message varchar(255) not null,
        phone varchar(255) not null,
        providerDate timestamp,
        valid boolean not null,
        mmeteoBox varchar(36),
        primary key (id)
    )

    alter table public.mmeteo_box 
        add constraint UK_mdw2wdtntwov029v3vjf7udgm  unique (imei)

    alter table public.mmeteo_box 
        add constraint FK_kkqqpjrbmmtaxqda44jb0pi 
        foreign key (customer) 
        references public.customer

    alter table public.mmeteo_box_data 
        add constraint FK_ebxqdker97v6n1bvgug44jd7n 
        foreign key (mmeteoBox) 
        references public.mmeteo_box

    alter table public.sms 
        add constraint FK_1edtqd3320935owxoaagtkxlg 
        foreign key (mmeteoBox) 
        references public.mmeteo_box
