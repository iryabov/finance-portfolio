SELECT :select
FROM dial d
JOIN asset s ON s.ticker = d.ticker
JOIN account a ON a.id = d.account_id
WHERE (d.active is null OR d.active = true)
  AND :filters
ORDER BY :sortings