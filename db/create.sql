CREATE DATABASE invest
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    CONNECTION LIMIT = 200;


CREATE TABLE public.account
(
    id numeric NOT NULL,
    name character varying NOT NULL,
    num character varying,
    CONSTRAINT account_pk PRIMARY KEY (id)
);

CREATE TABLE public.portfolio
(
    id numeric NOT NULL,
    name character varying NOT NULL,
    CONSTRAINT portfolio_pkey PRIMARY KEY (id)
);

CREATE TABLE public.asset
(
    ticker character varying NOT NULL,
    name character varying,
    class character varying,
    sector character varying COLLATE pg_catalog."default",
    country character varying COLLATE pg_catalog."default",
    currency character varying COLLATE pg_catalog."default",
    price_now double precision,
    price_week double precision,
    price_month double precision,
    CONSTRAINT asset_pkey PRIMARY KEY (ticker)
);

                 CREATE TABLE public.dial
(
    id numeric NOT NULL,
    active boolean,
    dt_open time without time zone NOT NULL,
    account_id numeric,
    portfolio_id numeric,
    ticker character varying COLLATE pg_catalog."default" NOT NULL,
    type character varying COLLATE pg_catalog."default" NOT NULL,
    currency character varying COLLATE pg_catalog."default" NOT NULL,
    amount numeric NOT NULL,
    quantity numeric NOT NULL,
    fee numeric,
    _sold_quantity numeric,
    _sold_amount numeric,
    dt_close timestamp without time zone,
    tax numeric,
    note character varying COLLATE pg_catalog."default",
    CONSTRAINT asset_pk PRIMARY KEY (id),
    CONSTRAINT account_fkey FOREIGN KEY (account_id)
        REFERENCES public.account (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID,
    CONSTRAINT portfolio_fkey FOREIGN KEY (portfolio_id)
        REFERENCES public.portfolio (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID,
    CONSTRAINT ticker_fkey FOREIGN KEY (ticker)
        REFERENCES public.asset (ticker) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);

CREATE TABLE public.flow
(
    id numeric NOT NULL,
    dial_from numeric NOT NULL,
    dial_to numeric NOT NULL,
    CONSTRAINT flow_pkey PRIMARY KEY (id),
    CONSTRAINT dial_from_fkey FOREIGN KEY (dial_from)
        REFERENCES public.dial (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID,
    CONSTRAINT dial_to_fkey FOREIGN KEY (dial_to)
        REFERENCES public.dial (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);

CREATE TABLE public.strategy
(
    id numeric NOT NULL,
    portfolio_id numeric NOT NULL,
    active boolean,
    ticker character varying COLLATE pg_catalog."default" NOT NULL,
    type character varying COLLATE pg_catalog."default",
    proportion numeric,
    take_profit double precision,
    stop_loss double precision,
    CONSTRAINT strategy_pkey PRIMARY KEY (id),
    CONSTRAINT portfolio_fkey FOREIGN KEY (portfolio_id)
        REFERENCES public.portfolio (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID,
    CONSTRAINT ticker_fkey FOREIGN KEY (ticker)
        REFERENCES public.asset (ticker) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);

CREATE TABLE public.idea
(
    id numeric NOT NULL,
    active boolean,
    ticker character varying COLLATE pg_catalog."default" NOT NULL,
    dt_open time without time zone,
    price_open double precision,
    currency character varying COLLATE pg_catalog."default",
    dt_close time without time zone,
    price_close double precision,
    yield_dividend numeric,
    yield_coupon numeric,
    fee_management numeric,
    fee_transaction numeric,
    tax numeric,
    note character varying COLLATE pg_catalog."default",
    CONSTRAINT idea_pkey PRIMARY KEY (id),
    CONSTRAINT ticker_fkey FOREIGN KEY (ticker)
        REFERENCES public.asset (ticker) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
        NOT VALID
);