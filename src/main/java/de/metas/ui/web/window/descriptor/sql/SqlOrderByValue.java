package de.metas.ui.web.window.descriptor.sql;

import java.util.Objects;

import org.adempiere.ad.expression.api.IExpressionEvaluator.OnVariableNotFound;
import org.adempiere.ad.expression.api.IStringExpression;
import org.adempiere.ad.expression.api.impl.ConstantStringExpression;
import org.compiere.util.Evaluatee;

import de.metas.util.Check;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2020 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

/** Field's SQL expression to be used in ORDER BY constructions */
@EqualsAndHashCode
@ToString
public class SqlOrderByValue
{
	public static SqlOrderByValue of(final String sql)
	{
		return SqlOrderByValue.builder().sql(sql).build();
	}

	private final SqlSelectDisplayValue sqlSelectDisplayValue;
	private final SqlSelectValue sqlSelectValue;
	private final String sql;

	private final String joinOnTableNameOrAlias;

	@Builder(toBuilder = true)
	private SqlOrderByValue(
			final SqlSelectDisplayValue sqlSelectDisplayValue,
			final SqlSelectValue sqlSelectValue,
			final String sql,
			final String joinOnTableNameOrAlias)
	{
		this.joinOnTableNameOrAlias = joinOnTableNameOrAlias;

		if (sqlSelectDisplayValue != null)
		{
			this.sqlSelectDisplayValue = sqlSelectDisplayValue.withJoinOnTableNameOrAlias(joinOnTableNameOrAlias);
			this.sqlSelectValue = null;
			this.sql = null;
		}
		else if (sqlSelectValue != null)
		{
			this.sqlSelectDisplayValue = null;
			this.sqlSelectValue = sqlSelectValue.withJoinOnTableNameOrAlias(joinOnTableNameOrAlias);
			this.sql = null;
		}
		else if (!Check.isEmpty(sql, true))
		{
			this.sqlSelectDisplayValue = null;
			this.sqlSelectValue = null;
			this.sql = sql;
		}
		else
		{
			this.sqlSelectDisplayValue = null;
			this.sqlSelectValue = null;
			this.sql = null;
		}
	}

	public SqlOrderByValue withJoinOnTableNameOrAlias(final String joinOnTableNameOrAlias)
	{
		return !Objects.equals(this.joinOnTableNameOrAlias, joinOnTableNameOrAlias)
				? toBuilder().joinOnTableNameOrAlias(joinOnTableNameOrAlias).build()
				: this;
	}

	public IStringExpression toStringExpression()
	{
		if (sqlSelectDisplayValue != null)
		{
			return sqlSelectDisplayValue.toStringExpression();
		}

		if (sqlSelectValue != null)
		{
			return ConstantStringExpression.of(sqlSelectValue.toSqlString());
		}

		if (sql != null)
		{
			return ConstantStringExpression.of(sql);
		}

		return IStringExpression.NULL;
	}

	public String evaluate(@NonNull final Evaluatee ctx)
	{
		return toStringExpression().evaluate(ctx, OnVariableNotFound.Fail);
	}

	public boolean isNullExpression()
	{
		return toStringExpression().isNullExpression();
	}
}
